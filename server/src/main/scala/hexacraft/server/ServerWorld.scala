package hexacraft.server

import hexacraft.server.ServerWorld.WorldTickResult
import hexacraft.util.*
import hexacraft.world.*
import hexacraft.world.block.{Block, BlockRepository, BlockState}
import hexacraft.world.chunk.*
import hexacraft.world.coord.*
import hexacraft.world.entity.{Entity, EntityPhysicsSystem}

import com.martomate.nbt.Nbt

import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

object ServerWorld {
  private val ticksBetweenBlockUpdates = 5
  private val ticksBetweenEntityRelocation = 120

  var shouldChillChunkLoader = false

  class WorldTickResult(
      val chunksAdded: Seq[ChunkRelWorld],
      val chunksRemoved: Seq[ChunkRelWorld],
      val chunksNeedingRenderUpdate: Seq[ChunkRelWorld],
      val blocksUpdated: Seq[BlockRelWorld],
      val entityEvents: Seq[(UUID, EntityEvent)]
  )
}

class ServerWorld(worldProvider: WorldProvider, val worldInfo: WorldInfo)
    extends BlockRepository
    with BlocksInWorldExtended {
  given size: CylinderSize = worldInfo.worldSize
  import scala.concurrent.ExecutionContext.Implicits.global

  private val backgroundTasks: mutable.ArrayBuffer[Future[Unit]] = mutable.ArrayBuffer.empty

  private val worldGenerator = new WorldGenerator(worldInfo.gen)
  private val worldPlanner: WorldPlanner = WorldPlanner(this, worldInfo.gen.seed)
  private val lightPropagator: LightPropagator = new LightPropagator(this, this.requestRenderUpdate)

  val renderDistance: Double = 8 * CylinderSize.y60

  val collisionDetector: CollisionDetector = new CollisionDetector(this)

  private val entityPhysicsSystem = EntityPhysicsSystem(this, collisionDetector)

  private val columns: mutable.LongMap[ChunkColumnTerrain] = mutable.LongMap.empty
  private val chunks: mutable.LongMap[Chunk] = mutable.LongMap.empty

  private val chunkLoadingPrioritizer = new ChunkLoadingPrioritizer(renderDistance)

  private val chunksLoading: mutable.Map[ChunkRelWorld, Future[(Chunk, Boolean)]] = mutable.Map.empty
  private val chunksUnloading: mutable.Map[ChunkRelWorld, Future[Unit]] = mutable.Map.empty

  private val blocksToUpdate: UniqueLongQueue = new UniqueLongQueue

  private val savedChunkModCounts = mutable.Map.empty[ChunkRelWorld, Long]

  private val chunksNeedingRenderUpdate = mutable.ArrayBuffer.empty[ChunkRelWorld]

  private val entityEventsSinceLastTick = mutable.ArrayBuffer.empty[(UUID, EntityEvent)]

  def getColumn(coords: ColumnRelWorld): Option[ChunkColumnTerrain] = {
    columns.get(coords.value)
  }

  def getChunk(coords: ChunkRelWorld): Option[Chunk] = {
    chunks.get(coords.value)
  }

  def loadedChunks: Seq[ChunkRelWorld] = {
    chunks.keys.map(c => ChunkRelWorld(c)).toSeq
  }

  def getBlock(coords: BlockRelWorld): BlockState = {
    getChunk(coords.getChunkRelWorld) match {
      case Some(chunk) => chunk.getBlock(coords.getBlockRelChunk)
      case None        => BlockState.Air
    }
  }

  def provideColumn(coords: ColumnRelWorld): ChunkColumnTerrain = {
    ensureColumnExists(coords)
  }

  def setBlock(coords: BlockRelWorld, block: BlockState): Unit = {
    getChunk(coords.getChunkRelWorld) match {
      case Some(chunk) =>
        chunk.setBlock(coords.getBlockRelChunk, block)
        onSetBlock(coords, block)
      case None =>
    }
  }

  def removeBlock(coords: BlockRelWorld): Unit = {
    getChunk(coords.getChunkRelWorld) match {
      case Some(chunk) =>
        chunk.setBlock(coords.getBlockRelChunk, BlockState.Air)
        onSetBlock(coords, BlockState.Air)
      case None =>
    }
  }

  def addEntity(entity: Entity): Unit = {
    chunkOfEntity(entity) match {
      case Some(chunk) =>
        chunk.addEntity(entity)
        entityEventsSinceLastTick += entity.id -> EntityEvent.Spawned(entity.toNBT)
      case None =>
    }
  }

  def removeEntity(entity: Entity): Unit = {
    chunkOfEntity(entity) match {
      case Some(chunk) =>
        chunk.removeEntity(entity)
        entityEventsSinceLastTick += entity.id -> EntityEvent.Despawned
      case None =>
    }
  }

  def removeAllEntities(): Unit = {
    for {
      ch <- chunks.values
      e <- ch.entities.toSeq
    } do {
      ch.removeEntity(e)
      entityEventsSinceLastTick += e.id -> EntityEvent.Despawned
    }
  }

  private def chunkOfEntity(entity: Entity): Option[Chunk] = {
    getChunk(CoordUtils.approximateChunkCoords(entity.transform.position))
  }

  def getHeight(x: Int, z: Int): Int = {
    val coords = ColumnRelWorld(x >> 4, z >> 4)
    ensureColumnExists(coords).terrainHeight.getHeight(x & 15, z & 15)
  }

  def setChunk(chunkCoords: ChunkRelWorld, ch: Chunk): Unit = {
    val col = ensureColumnExists(chunkCoords.getColumnRelWorld)
    setChunkAndUpdateHeightmap(col, chunkCoords, ch)

    worldPlanner.prepare(chunkCoords)
    worldPlanner.decorate(chunkCoords, ch)

    if ch.modCount != savedChunkModCounts.getOrElse(chunkCoords, -1L) then {
      val chunkNbt = ch.toNbt
      backgroundTasks += Future(worldProvider.saveChunkData(chunkNbt, chunkCoords))
      savedChunkModCounts(chunkCoords) = ch.modCount
      updateHeightmapAfterChunkUpdate(col, chunkCoords, ch)
    }

    ch.initLightingIfNeeded(chunkCoords, lightPropagator)

    requestRenderUpdate(chunkCoords)
    requestRenderUpdateForNeighborChunks(chunkCoords)

    val allBlocks = ch.blocks

    // Add all blocks in the chunk to the update queue
    val newBlockUpdates = new mutable.ArrayBuffer[Long](allBlocks.length)
    var bIdx = 0
    while bIdx < allBlocks.length do {
      newBlockUpdates += BlockRelWorld.fromChunk(allBlocks(bIdx).coords, chunkCoords).value
      bIdx += 1
    }
    blocksToUpdate.enqueueMany(newBlockUpdates)
    newBlockUpdates.clear()

    // Add all blocks in neighboring chunks to the update queue
    bIdx = 0
    while bIdx < allBlocks.length do {
      val block = allBlocks(bIdx)

      var side = 0
      while side < 8 do {
        if block.coords.isOnChunkEdge(side) then {
          val neighCoords = block.coords.neighbor(side)
          val neighChunkCoords = chunkCoords.offset(ChunkRelWorld.neighborOffsets(side))

          if getChunk(neighChunkCoords).isDefined then {
            newBlockUpdates += BlockRelWorld.fromChunk(neighCoords, neighChunkCoords).value
          }
        }
        side += 1
      }
      bIdx += 1
    }
    blocksToUpdate.enqueueMany(newBlockUpdates)
  }

  private def updateHeightmapAfterChunkUpdate(col: ChunkColumnTerrain, chunkCoords: ChunkRelWorld, chunk: Chunk)(using
      CylinderSize
  ): Unit = {
    for {
      cx <- 0 until 16
      cz <- 0 until 16
    } do {
      val blockCoords = BlockRelChunk(cx, 15, cz)
      updateHeightmapAfterBlockUpdate(
        col,
        BlockRelWorld.fromChunk(blockCoords, chunkCoords),
        chunk.getBlock(blockCoords)
      )
    }
  }

  private def updateHeightmapAfterBlockUpdate(col: ChunkColumnTerrain, coords: BlockRelWorld, now: BlockState): Unit = {
    val height = col.terrainHeight.getHeight(coords.cx, coords.cz)

    if coords.y >= height then {
      if now.blockType != Block.Air then {
        col.terrainHeight.setHeight(coords.cx, coords.cz, coords.y.toShort)
      } else {
        val newHeight = LazyList
          .range((height - 1).toShort, Short.MinValue, -1.toShort)
          .map(y =>
            chunks
              .get(ChunkRelWorld(coords.X.toInt, y >> 4, coords.Z.toInt).value)
              .map(chunk => (y, chunk.getBlock(BlockRelChunk(coords.cx, y & 15, coords.cz))))
              .orNull
          )
          .takeWhile(_ != null) // stop searching if the chunk is not loaded
          .collectFirst({ case (y, block) if block.blockType != Block.Air => y })
          .getOrElse(Short.MinValue)

        col.terrainHeight.setHeight(coords.cx, coords.cz, newHeight)
      }
    }
  }

  private def updateHeightmapAfterChunkReplaced(
      heightMap: ChunkColumnHeightMap,
      chunkCoords: ChunkRelWorld,
      chunk: Chunk
  ): Unit = {
    val yy = chunkCoords.Y.toInt * 16
    for x <- 0 until 16 do {
      for z <- 0 until 16 do {
        val height = heightMap.getHeight(x, z)

        val highestBlockY = (yy + 15 to yy by -1)
          .filter(_ > height)
          .find(y => chunk.getBlock(BlockRelChunk(x, y, z)).blockType != Block.Air)

        highestBlockY match {
          case Some(h) => heightMap.setHeight(x, z, h.toShort)
          case None    =>
        }
      }
    }
  }

  private def setChunkAndUpdateHeightmap(col: ChunkColumnTerrain, chunkCoords: ChunkRelWorld, chunk: Chunk): Unit = {
    chunks.put(chunkCoords.value, chunk) match {
      case Some(`chunk`)  => // the chunk is not new so nothing needs to be done
      case Some(oldChunk) => updateHeightmapAfterChunkReplaced(col.terrainHeight, chunkCoords, chunk)
      case None           => updateHeightmapAfterChunkReplaced(col.terrainHeight, chunkCoords, chunk)
    }
  }

  def removeChunk(chunkCoords: ChunkRelWorld): Boolean = {
    val columnCoords = chunkCoords.getColumnRelWorld

    var chunkWasRemoved = false

    val columnOpt = columns.get(columnCoords.value)
    if columnOpt.isDefined then {
      val col = columnOpt.get

      val removedChunkOpt = chunks.remove(chunkCoords.value)
      if removedChunkOpt.isDefined then {
        val removedChunk = removedChunkOpt.get

        chunkWasRemoved = true

        if removedChunk.modCount != savedChunkModCounts.getOrElse(chunkCoords, -1L) then {
          val removedChunkNbt = removedChunk.toNbt
          backgroundTasks += Future(worldProvider.saveChunkData(removedChunkNbt, chunkCoords))
          savedChunkModCounts -= chunkCoords
        }
        requestRenderUpdateForNeighborChunks(chunkCoords)
      }

      if chunks.keys.count(v => ChunkRelWorld(v).getColumnRelWorld == columnCoords) == 0 then {
        columns.remove(columnCoords.value)
        backgroundTasks += Future(saveColumn(columnCoords, col))
      }
    }

    chunkWasRemoved
  }

  def tick(cameras: Seq[Camera]): WorldTickResult = {
    val (chunksAdded, chunksRemoved) = performChunkLoading(cameras)

    val blocksUpdated = if blockUpdateTimer.tick() then {
      performBlockUpdates()
    } else Seq.empty
    if relocateEntitiesTimer.tick() then {
      performEntityRelocation()
    }

    val chIt = chunks.values.iterator
    while chIt.hasNext do {
      val ch = chIt.next

      ch.optimizeStorage()
      tickEntities(ch.entities)
    }

    val entityEvents = mutable.ArrayBuffer.empty[(UUID, EntityEvent)]
    entityEvents ++= entityEventsSinceLastTick
    entityEventsSinceLastTick.clear()

    /*
    for ch <- chunks.values do {
      for e <- ch.entities do {
        entityEvents += e.id -> EntityEvent.Position(e.transform.position)
      }
    }
     */

    val r = chunksNeedingRenderUpdate.toSeq
    chunksNeedingRenderUpdate.clear()

    new WorldTickResult(chunksAdded, chunksRemoved, r, blocksUpdated, entityEvents.toSeq)
  }

  private def performChunkLoading(cameras: Seq[Camera]): (Seq[ChunkRelWorld], Seq[ChunkRelWorld]) = {
    if cameras.isEmpty then {
      return (Nil, Nil)
    }

    chunkLoadingPrioritizer.tick(PosAndDir.fromCameraView(cameras.head.view))

    val chunksToLoadPerTick = 8
    val chunksToUnloadPerTick = 12

    for _ <- 1 to chunksToLoadPerTick do {
      val maxQueueLength = if ServerWorld.shouldChillChunkLoader then 1 else 16
      if chunksLoading.size < maxQueueLength then {
        chunkLoadingPrioritizer.popChunkToLoad() match {
          case Some(coords) =>
            chunksLoading(coords) = Future(worldProvider.loadChunkData(coords)).map {
              case Some(loadedTag) => (Chunk.fromNbt(loadedTag), false)
              case None =>
                val column = provideColumn(coords.getColumnRelWorld)
                (Chunk.fromGenerator(coords, column, worldGenerator), true)
            }
          case None =>
        }
      }
    }

    for _ <- 1 to chunksToUnloadPerTick do {
      val maxQueueLength = if ServerWorld.shouldChillChunkLoader then 2 else 20
      if chunksUnloading.size < maxQueueLength then {
        chunkLoadingPrioritizer.popChunkToRemove() match {
          case Some(coords) =>
            getChunk(coords) match {
              case Some(chunk) =>
                if chunk.modCount != savedChunkModCounts.getOrElse(coords, -1L) then {
                  savedChunkModCounts(coords) = chunk.modCount

                  chunksUnloading(coords) = Future(worldProvider.saveChunkData(chunk.toNbt, coords))
                } else { // The chunk has not changed, so no need to save it to disk, but still unload it
                  chunksUnloading(coords) = Future.successful(())
                }
              case None =>
            }
          case None =>
        }
      }
    }

    val chunksAdded = mutable.ArrayBuffer.empty[ChunkRelWorld]
    val chunksRemoved = mutable.ArrayBuffer.empty[ChunkRelWorld]

    val lIt = chunksFinishedLoading.iterator
    while lIt.hasNext do {
      val (chunkCoords, chunk, isNew) = lIt.next
      savedChunkModCounts(chunkCoords) = if isNew then -1L else chunk.modCount
      setChunk(chunkCoords, chunk)
      chunksAdded += chunkCoords
    }

    val uIt = chunksFinishedUnloading.iterator
    while uIt.hasNext do {
      val chunkCoords = uIt.next
      val chunkWasRemoved = removeChunk(chunkCoords)
      if chunkWasRemoved then {
        chunksRemoved += chunkCoords
      }
    }

    (chunksAdded.toSeq, chunksRemoved.toSeq)
  }

  private def chunksFinishedLoading: Seq[(ChunkRelWorld, Chunk, Boolean)] = {
    val chunksToAdd = mutable.ArrayBuffer.empty[(ChunkRelWorld, Chunk, Boolean)]
    for (chunkCoords, futureChunk) <- chunksLoading do {
      futureChunk.value match {
        case None => // future is not ready yet
        case Some(result) =>
          result match {
            case Failure(_) => // TODO: handle error
            case Success((chunk, isNew)) =>
              chunksToAdd += ((chunkCoords, chunk, isNew))
          }
      }
    }
    val finishedChunks = chunksToAdd.toSeq
    for (coords, _, _) <- finishedChunks do {
      chunksLoading -= coords
    }
    finishedChunks
  }

  private def chunksFinishedUnloading: Seq[ChunkRelWorld] = {
    val chunksToRemove = mutable.ArrayBuffer.empty[ChunkRelWorld]
    for (chunkCoords, future) <- chunksUnloading do {
      future.value match {
        case None => // future is not ready yet
        case Some(result) =>
          result match {
            case Failure(_) => // TODO: handle error
            case Success(_) =>
              chunksToRemove += chunkCoords
          }
      }
    }
    val finishedChunks = chunksToRemove.toSeq
    for coords <- finishedChunks do {
      chunksUnloading -= coords
    }
    finishedChunks
  }

  private def saveColumn(columnCoords: ColumnRelWorld, col: ChunkColumnTerrain): Unit = {
    worldProvider.saveColumnData(ChunkColumnData(Some(col.terrainHeight)).toNBT, columnCoords)
  }

  @tailrec // this is done for performance
  private def tickEntities(ents: Iterable[Entity]): Unit = {
    if ents.nonEmpty then {
      tickEntity(ents.head)
      tickEntities(ents.tail)
    }
  }

  private def tickEntity(e: Entity): Unit = {
    e.ai match {
      case Some(ai) =>
        ai.tick(this, e.transform, e.motion, e.boundingBox)
        e.motion.velocity.add(ai.acceleration)
      case None =>
    }

    e.motion.velocity.x *= 0.9
    e.motion.velocity.z *= 0.9

    entityPhysicsSystem.update(e.transform, e.motion, e.boundingBox)

    e.model.foreach(_.tick(e.motion.velocity.lengthSquared() > 0.1))
  }

  private val blockUpdateTimer: TickableTimer = TickableTimer(ServerWorld.ticksBetweenBlockUpdates)

  private def performBlockUpdates(): Seq[BlockRelWorld] = {
    val recordingWorld = RecordingBlockRepository(this)

    val blocksToUpdateNow = new ArrayBuffer[Long](blocksToUpdate.size)
    blocksToUpdate.drainInto(value => blocksToUpdateNow += value)

    var bIdx = 0
    while bIdx < blocksToUpdateNow.length do {
      val c = BlockRelWorld(blocksToUpdateNow(bIdx))

      val block = getBlock(c).blockType
      val behaviour = block.behaviour
      if behaviour.isDefined then {
        behaviour.get.onUpdated(c, block, recordingWorld)
      }

      bIdx += 1
    }

    recordingWorld.collectUpdates.distinct
  }

  private val relocateEntitiesTimer: TickableTimer = TickableTimer(ServerWorld.ticksBetweenEntityRelocation)

  private def performEntityRelocation(): Unit = {
    val entList = for {
      ch <- chunks.values
      ent <- ch.entities
    } yield (ch, ent, chunkOfEntity(ent))

    for {
      (ch, ent, newOpt) <- entList
      newChunk <- newOpt
      if newChunk != ch
    } do {
      ch.removeEntity(ent)
      newChunk.addEntity(ent)
    }
  }

  private def requestRenderUpdateForNeighborChunks(coords: ChunkRelWorld): Unit = {
    for side <- 0 until 8 do {
      val nCoords = coords.offset(NeighborOffsets(side))
      if getChunk(nCoords).isDefined then {
        requestRenderUpdate(nCoords)
      }
    }
  }

  private def ensureColumnExists(here: ColumnRelWorld): ChunkColumnTerrain = {
    columns.get(here.value) match {
      case Some(col) => col
      case None =>
        val col = ChunkColumnTerrain.create(
          ChunkColumnHeightMap.fromData2D(worldGenerator.getHeightmapInterpolator(here)),
          worldProvider.loadColumnData(here).map(ChunkColumnData.fromNbt)
        )
        columns(here.value) = col
        col
    }
  }

  def getBrightness(block: BlockRelWorld): Float = {
    getChunk(block.getChunkRelWorld) match {
      case Some(c) => c.lighting.getBrightness(block.getBlockRelChunk)
      case None    => 1.0f
    }
  }

  private def requestRenderUpdate(chunkCoords: ChunkRelWorld): Unit = {
    chunksNeedingRenderUpdate += chunkCoords
  }

  def unload(): Unit = {
    blockUpdateTimer.enabled = false

    for (chunkKey, chunk) <- chunks do {
      val chunkCoords = ChunkRelWorld(chunkKey)

      if chunk.modCount != savedChunkModCounts.getOrElse(chunkCoords, -1L) then {
        val chunkNbt = chunk.toNbt
        backgroundTasks += Future(worldProvider.saveChunkData(chunkNbt, chunkCoords))
      }
    }

    chunks.clear()

    for (columnKey, column) <- columns do {
      backgroundTasks += Future(saveColumn(ColumnRelWorld(columnKey), column))
    }

    columns.clear()

    for t <- backgroundTasks do {
      Await.result(t, Duration(10, TimeUnit.SECONDS))
    }

    for f <- chunksLoading.values do {
      Await.result(f, Duration(10, TimeUnit.SECONDS))
    }
    for f <- chunksUnloading.values do {
      Await.result(f, Duration(10, TimeUnit.SECONDS))
    }
  }

  private def requestBlockUpdate(coords: BlockRelWorld): Unit = {
    blocksToUpdate.enqueue(coords.value)
  }

  private def onSetBlock(coords: BlockRelWorld, block: BlockState): Unit = {
    def affectedChunkOffset(where: Byte): Int = {
      where match {
        case 0  => -1
        case 15 => 1
        case _  => 0
      }
    }

    def isInNeighborChunk(chunkOffset: Offset) = {
      val xx = affectedChunkOffset(coords.cx)
      val yy = affectedChunkOffset(coords.cy)
      val zz = affectedChunkOffset(coords.cz)

      chunkOffset.dx * xx == 1 || chunkOffset.dy * yy == 1 || chunkOffset.dz * zz == 1
    }

    for col <- columns.get(coords.getColumnRelWorld.value) do {
      updateHeightmapAfterBlockUpdate(col, coords, block)
    }

    val cCoords = coords.getChunkRelWorld
    val bCoords = coords.getBlockRelChunk

    for c <- getChunk(cCoords) do {
      handleLightingOnSetBlock(cCoords, c, bCoords, block)

      requestRenderUpdate(cCoords)
      requestBlockUpdate(coords)

      for s <- 0 until 8 do {
        val neighCoords = bCoords.globalNeighbor(s, cCoords)
        val neighChunkCoords = neighCoords.getChunkRelWorld

        if neighChunkCoords != cCoords then {
          for n <- getChunk(neighChunkCoords) do {
            requestRenderUpdate(neighChunkCoords)
            requestBlockUpdate(neighCoords)
          }
        } else {
          requestBlockUpdate(neighCoords)
        }
      }
    }
  }

  private def handleLightingOnSetBlock(
      chunkCoords: ChunkRelWorld,
      chunk: Chunk,
      blockCoords: BlockRelChunk,
      block: BlockState
  ): Unit = {
    lightPropagator.removeTorchlight(chunkCoords, chunk, blockCoords)
    lightPropagator.removeSunlight(chunkCoords, chunk, blockCoords)
    if block.blockType.lightEmitted != 0 then {
      lightPropagator.addTorchlight(chunkCoords, chunk, blockCoords, block.blockType.lightEmitted)
    }
  }
}