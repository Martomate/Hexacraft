package hexacraft.world

import hexacraft.math.bits.Int12
import hexacraft.util.*
import hexacraft.world.World.WorldTickResult
import hexacraft.world.block.{Block, BlockRepository, BlockState}
import hexacraft.world.chunk.*
import hexacraft.world.coord.*
import hexacraft.world.entity.{Entity, EntityPhysicsSystem}

import com.martomate.nbt.Nbt

import java.util.concurrent.TimeUnit
import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

object World {
  private val ticksBetweenBlockUpdates = 5
  private val ticksBetweenEntityRelocation = 120

  var shouldChillChunkLoader = false

  class WorldTickResult(
      val chunksAdded: Seq[ChunkRelWorld],
      val chunksRemoved: Seq[ChunkRelWorld],
      val chunksNeedingRenderUpdate: Seq[ChunkRelWorld]
  )
}

class World(worldProvider: WorldProvider, val worldInfo: WorldInfo) extends BlockRepository with BlocksInWorld {
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

  private val blocksToUpdate: UniqueQueue[BlockRelWorld] = new UniqueQueue

  private val savedChunkModCounts = mutable.Map.empty[ChunkRelWorld, Long]

  private val chunksNeedingRenderUpdate = mutable.ArrayBuffer.empty[ChunkRelWorld]

  def getColumn(coords: ColumnRelWorld): Option[ChunkColumnTerrain] = {
    columns.get(coords.value)
  }

  def getChunk(coords: ChunkRelWorld): Option[Chunk] = {
    chunks.get(coords.value)
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
      case Some(chunk) => chunk.addEntity(entity)
      case None        =>
    }
  }

  def removeEntity(entity: Entity): Unit = {
    chunkOfEntity(entity) match {
      case Some(chunk) => chunk.removeEntity(entity)
      case None        =>
    }
  }

  def removeAllEntities(): Unit = {
    for {
      ch <- chunks.values
      e <- ch.entities.toSeq
    } do {
      ch.removeEntity(e)
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

    for block <- ch.blocks do {
      requestBlockUpdate(BlockRelWorld.fromChunk(block.coords, chunkCoords))

      for side <- 0 until 8 do {
        if block.coords.isOnChunkEdge(side) then {
          val neighCoords = block.coords.neighbor(side)
          val neighChunkCoords = chunkCoords.offset(ChunkRelWorld.neighborOffsets(side))

          for neighbor <- getChunk(neighChunkCoords) do {
            requestBlockUpdate(BlockRelWorld.fromChunk(neighCoords, neighChunkCoords))
          }
        }
      }
    }
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

    for col <- columns.get(columnCoords.value) do {
      for removedChunk <- chunks.remove(chunkCoords.value) do {
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

    if blockUpdateTimer.tick() then {
      performBlockUpdates()
    }
    if relocateEntitiesTimer.tick() then {
      performEntityRelocation()
    }

    for ch <- chunks.values do {
      ch.optimizeStorage()
      tickEntities(ch.entities)
    }

    val r = chunksNeedingRenderUpdate.toSeq
    chunksNeedingRenderUpdate.clear()

    new WorldTickResult(chunksAdded, chunksRemoved, r)
  }

  private def performChunkLoading(cameras: Seq[Camera]): (Seq[ChunkRelWorld], Seq[ChunkRelWorld]) = {
    if cameras.isEmpty then {
      return (Nil, Nil)
    }

    chunkLoadingPrioritizer.tick(PosAndDir.fromCameraView(cameras.head.view))

    val chunksToLoadPerTick = 4
    val chunksToUnloadPerTick = 6

    for _ <- 1 to chunksToLoadPerTick do {
      val maxQueueLength = if World.shouldChillChunkLoader then 1 else 8
      if chunksLoading.size < maxQueueLength then {
        chunkLoadingPrioritizer.popChunkToLoad() match {
          case Some(coords) =>
            chunksLoading(coords) = Future(worldProvider.loadChunkData(coords)).map {
              case Some(loadedTag) => (Chunk.fromNbt(loadedTag), false)
              case None            => (Chunk.fromGenerator(coords, this, worldGenerator), true)
            }
          case None =>
        }
      }
    }

    for _ <- 1 to chunksToUnloadPerTick do {
      val maxQueueLength = if World.shouldChillChunkLoader then 2 else 10
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

    for (chunkCoords, chunk, isNew) <- chunksFinishedLoading do {
      savedChunkModCounts(chunkCoords) = if isNew then -1L else chunk.modCount

      setChunk(chunkCoords, chunk)

      chunksAdded += chunkCoords
    }
    for chunkCoords <- chunksFinishedUnloading do {
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
        ai.tick(this, e.transform, e.velocity, e.boundingBox)
        e.velocity.velocity.add(ai.acceleration)
      case None =>
    }

    e.velocity.velocity.x *= 0.9
    e.velocity.velocity.z *= 0.9

    entityPhysicsSystem.update(e.transform, e.velocity, e.boundingBox)

    e.model.foreach(_.tick(e.velocity.velocity.lengthSquared() > 0.1))
  }

  private val blockUpdateTimer: TickableTimer = TickableTimer(World.ticksBetweenBlockUpdates)

  private def performBlockUpdates(): Unit = {
    val blocksToUpdateNow = ArrayBuffer.empty[BlockRelWorld]
    while !blocksToUpdate.isEmpty do {
      blocksToUpdateNow += blocksToUpdate.dequeue()
    }
    for c <- blocksToUpdateNow do {
      val block = getBlock(c).blockType
      block.behaviour.foreach(_.onUpdated(c, block, this))
    }
  }

  private val relocateEntitiesTimer: TickableTimer = TickableTimer(World.ticksBetweenEntityRelocation)

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
    blocksToUpdate.enqueue(coords)
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
