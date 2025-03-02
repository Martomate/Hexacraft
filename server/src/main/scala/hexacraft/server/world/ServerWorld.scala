package hexacraft.server.world

import hexacraft.nbt.Nbt
import hexacraft.server.world.ServerWorld.WorldTickResult
import hexacraft.server.world.plan.WorldPlanner
import hexacraft.util.*
import hexacraft.world.*
import hexacraft.world.block.{Block, BlockRepository, BlockState}
import hexacraft.world.chunk.*
import hexacraft.world.coord.*
import hexacraft.world.entity.{Entity, EntityPhysicsSystem}

import java.util.UUID
import java.util.concurrent.{Executors, TimeUnit}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Await, ExecutionContext, Future}
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

  private val fsExecutorService = Executors.newFixedThreadPool(8, NamedThreadFactory("server-fs"))
  private val genExecutorService = Executors.newFixedThreadPool(4, NamedThreadFactory("server-gen"))

  private val fsAsync: ExecutionContext = ExecutionContext.fromExecutor(fsExecutorService)
  private val genAsync: ExecutionContext = ExecutionContext.fromExecutor(genExecutorService)

  private val backgroundTasks: mutable.ArrayBuffer[Future[Unit]] = mutable.ArrayBuffer.empty

  private val worldGenerator = new WorldGenerator(worldInfo.gen)
  private val worldPlanner: WorldPlanner = WorldPlanner(this, worldInfo.gen.seed)
  private val lightPropagator: LightPropagator = new LightPropagator(this, this.requestRenderUpdate)

  val renderDistance: Double = 8 * CylinderSize.y60

  val collisionDetector: CollisionDetector = new CollisionDetector(this)

  private val entityPhysicsSystem = EntityPhysicsSystem(this, collisionDetector)

  private val columns: mutable.LongMap[ChunkColumnTerrain] = mutable.LongMap.empty

  // Note: these two must be updated together
  private val chunks: mutable.LongMap[Chunk] = mutable.LongMap.empty
  private val chunkList: mutable.ArrayBuffer[Chunk] = mutable.ArrayBuffer.empty // used for iteration

  private val chunkLoadingPrioritizer = new ChunkLoadingPrioritizer(renderDistance)

  private val chunksLoading: mutable.Map[ChunkRelWorld, Future[(Chunk, Boolean)]] = mutable.Map.empty
  private val chunksUnloading: mutable.Map[ChunkRelWorld, Future[Unit]] = mutable.Map.empty

  private val columnsLoading: mutable.LongMap[Future[ChunkColumnTerrain]] = mutable.LongMap.empty

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
      ch <- chunks.values // TODO: replace with chunkList once the race condition is fixed
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
      backgroundTasks += Future(worldProvider.saveChunkData(chunkNbt, chunkCoords))(using fsAsync)
      savedChunkModCounts(chunkCoords) = ch.modCount
      updateHeightmapAfterChunkUpdate(col, chunkCoords, ch)
    }

    ch.initLightingIfNeeded(chunkCoords, lightPropagator)

    requestRenderUpdate(chunkCoords)
    requestRenderUpdateForNeighborChunks(chunkCoords)

    val allBlocks = ch.blocks

    // Add all blocks in the chunk to the update queue
    val newBlockUpdates = new mutable.ArrayBuffer[Long](allBlocks.length)
    Loop.array(allBlocks) { block =>
      newBlockUpdates += BlockRelWorld.fromChunk(block.coords, chunkCoords).value
    }
    blocksToUpdate.enqueueMany(newBlockUpdates)
    newBlockUpdates.clear()

    // Add all blocks in neighboring chunks to the update queue
    Loop.array(allBlocks) { block =>
      Loop.rangeUntil(0, 8) { side =>
        if block.coords.isOnChunkEdge(side) then {
          val neighCoords = block.coords.neighbor(side)
          val neighChunkCoords = chunkCoords.offset(ChunkRelWorld.neighborOffsets(side))

          if getChunk(neighChunkCoords).isDefined then {
            newBlockUpdates += BlockRelWorld.fromChunk(neighCoords, neighChunkCoords).value
          }
        }
      }
    }
    blocksToUpdate.enqueueMany(newBlockUpdates)
  }

  private def updateHeightmapAfterChunkUpdate(col: ChunkColumnTerrain, chunkCoords: ChunkRelWorld, chunk: Chunk)(using
      CylinderSize
  ): Unit = {
    Loop.rangeUntil(0, 16) { cx =>
      Loop.rangeUntil(0, 16) { cz =>
        val blockCoords = BlockRelChunk(cx, 15, cz)
        updateHeightmapAfterBlockUpdate(
          col,
          BlockRelWorld.fromChunk(blockCoords, chunkCoords),
          chunk.getBlock(blockCoords)
        )
      }
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
    Loop.rangeUntil(0, 16) { x =>
      Loop.rangeUntil(0, 16) { z =>
        val height = heightMap.getHeight(x, z)

        if height < yy + 15 then {
          val highestBlockY = findHighest(
            math.max(yy, height),
            yy + 15,
            y => chunk.getBlock(BlockRelChunk(x, y, z)).blockType != Block.Air
          )

          highestBlockY match {
            case Some(h) => heightMap.setHeight(x, z, h.toShort)
            case None    =>
          }
        }
      }
    }
  }

  private inline def findHighest(lo: Int, hi: Int, inline p: Int => Boolean): Option[Int] = {
    var res: Option[Int] = None
    var i = hi
    while i >= lo do {
      if p(i) then {
        res = Some(i)
        i = lo
      }
      i -= 1
    }
    res
  }

  private def setChunkAndUpdateHeightmap(col: ChunkColumnTerrain, chunkCoords: ChunkRelWorld, chunk: Chunk): Unit = {
    chunks.put(chunkCoords.value, chunk) match {
      case Some(`chunk`) => // the chunk is not new so nothing needs to be done
      case Some(oldChunk) =>
        val oldIdx = this.chunkList.indexOfRef(oldChunk)
        this.chunkList(oldIdx) = chunk
        updateHeightmapAfterChunkReplaced(col.terrainHeight, chunkCoords, chunk)
      case None =>
        this.chunkList += chunk
        updateHeightmapAfterChunkReplaced(col.terrainHeight, chunkCoords, chunk)
    }
  }

  extension [A <: AnyRef](arr: mutable.ArrayBuffer[A]) {
    private def indexOfRef(elem: A): Int = {
      Loop.rangeUntil(0, arr.length) { i =>
        if arr(i).eq(elem) then return i
      }
      -1
    }
  }

  private def removeChunk(chunkCoords: ChunkRelWorld): Boolean = {
    val columnCoords = chunkCoords.getColumnRelWorld

    var chunkWasRemoved = false

    val columnOpt = columns.get(columnCoords.value)
    if columnOpt.isDefined then {
      val col = columnOpt.get

      val removedChunkOpt = chunks.remove(chunkCoords.value)
      if removedChunkOpt.isDefined then {
        val removedChunk = removedChunkOpt.get

        {
          // remove `removedChunk` from `chunkList` by replacing it with the last element
          val dst = this.chunkList.indexOf(removedChunk)
          val src = this.chunkList.size - 1
          val removed = this.chunkList.remove(src)
          if dst != src then {
            this.chunkList(dst) = removed
          }
        }

        chunkWasRemoved = true

        if removedChunk.modCount != savedChunkModCounts.getOrElse(chunkCoords, -1L) then {
          val removedChunkNbt = removedChunk.toNbt
          backgroundTasks += Future(worldProvider.saveChunkData(removedChunkNbt, chunkCoords))(using fsAsync)
          savedChunkModCounts -= chunkCoords
        }
        requestRenderUpdateForNeighborChunks(chunkCoords)
      }

      if chunks.keys.count(v => ChunkRelWorld(v).getColumnRelWorld == columnCoords) == 0 then {
        columns.remove(columnCoords.value)
        backgroundTasks += Future(saveColumn(columnCoords, col))(using fsAsync)
      }
    }

    chunkWasRemoved
  }

  def tick(
      cameras: Seq[Camera],
      requestedLoads: Seq[ChunkRelWorld],
      requestedUnloads: Seq[ChunkRelWorld]
  ): WorldTickResult = {
    handleLoadingColumns()

    val (chunksAdded, chunksRemoved) = performChunkLoading(requestedLoads, requestedUnloads)

    val blocksUpdated = if blockUpdateTimer.tick() then {
      performBlockUpdates()
    } else Seq.empty
    if relocateEntitiesTimer.tick() then {
      performEntityRelocation()
    }

    tickChunks()

    val entityEvents = mutable.ArrayBuffer.empty[(UUID, EntityEvent)]
    entityEvents ++= entityEventsSinceLastTick
    entityEventsSinceLastTick.clear()

    val r = chunksNeedingRenderUpdate.toSeq
    chunksNeedingRenderUpdate.clear()

    new WorldTickResult(chunksAdded, chunksRemoved, r, blocksUpdated, entityEvents.toSeq)
  }

  private def tickChunks(): Unit = {
    Loop.array(chunkList) { ch =>
      ch.optimizeStorage()
    }

    Loop.array(this.getAllEntities) { e =>
      tickEntity(e)
    }
  }

  private def getAllEntities: mutable.ArrayBuffer[Entity] = {
    val res = mutable.ArrayBuffer.empty[Entity]
    Loop.array(chunkList) { ch =>
      if ch.hasEntities then {
        ch.foreachEntity(res += _)
      }
    }
    res
  }

  private def performChunkLoading(
      requestedLoads: Seq[ChunkRelWorld],
      requestedUnloads: Seq[ChunkRelWorld]
  ): (Seq[ChunkRelWorld], Seq[ChunkRelWorld]) = {
    val chunksToLoadPerTick = if ServerWorld.shouldChillChunkLoader then 1 else 4
    val chunksToUnloadPerTick = if ServerWorld.shouldChillChunkLoader then 2 else 6

    var unloadsLeft = chunksToUnloadPerTick
    for coords <- requestedUnloads do {
      if unloadsLeft > 0 then {
        if !requestedLoads.contains(coords) && !chunksUnloading.contains(coords) && !chunksLoading.contains(coords)
        then {
          getChunk(coords) match {
            case Some(chunk) =>
              if chunk.modCount != savedChunkModCounts.getOrElse(coords, -1L) then {
                savedChunkModCounts(coords) = chunk.modCount

                unloadsLeft -= 1
                chunksUnloading(coords) = Future(worldProvider.saveChunkData(chunk.toNbt, coords))(using fsAsync)
              } else { // The chunk has not changed, so no need to save it to disk, but still unload it
                chunksUnloading(coords) = Future.successful(())
              }
            case None =>
          }
        }
      }
    }

    var loadsLeft = chunksToLoadPerTick
    for coords <- requestedLoads do {
      if loadsLeft > 0 then {
        if !chunksLoading.contains(coords) && !chunksUnloading.contains(coords) then {
          // make sure the columns around the chunk are getting loaded
          startLoadingColumnIfNeeded(coords.getColumnRelWorld)

          columns.get(coords.getColumnRelWorld.value) match { // the column is needed for the chunk to load
            case Some(column) =>
              loadsLeft -= 1
              chunksLoading(coords) = Future(worldProvider.loadChunkData(coords))(using fsAsync).flatMap {
                case Some(loadedTag) =>
                  Future((Chunk.fromNbt(loadedTag), false))(using genAsync)
                case None =>
                  Future((Chunk.fromGenerator(coords, column, worldGenerator), true))(using genAsync)
              }(using genAsync)
            case None =>
          }
        }
      }
    }

    val chunksAdded = mutable.ArrayBuffer.empty[ChunkRelWorld]
    val chunksRemoved = mutable.ArrayBuffer.empty[ChunkRelWorld]

    Loop.array(chunksFinishedLoading) { (chunkCoords, chunk, isNew) =>
      savedChunkModCounts(chunkCoords) = if isNew then -1L else chunk.modCount
      setChunk(chunkCoords, chunk)
      chunksAdded += chunkCoords
    }

    Loop.array(chunksFinishedUnloading) { chunkCoords =>
      val chunkWasRemoved = removeChunk(chunkCoords)
      if chunkWasRemoved then {
        chunksRemoved += chunkCoords
      }
    }

    (chunksAdded.toSeq, chunksRemoved.toSeq)
  }

  private def chunksFinishedLoading: IndexedSeq[(ChunkRelWorld, Chunk, Boolean)] = {
    val chunksToAdd = mutable.ArrayBuffer.empty[(ChunkRelWorld, Chunk, Boolean)]
    for (chunkCoords, futureChunk) <- chunksLoading do {
      futureChunk.value match {
        case None => // future is not ready yet
        case Some(result) =>
          result match {
            case Failure(_) => // TODO: handle error
            case Success((chunk, isNew)) =>
              if ensurePlannerCanPlanSoon(chunkCoords) then {
                chunksToAdd += ((chunkCoords, chunk, isNew))
              }
          }
      }
    }
    val finishedChunks = chunksToAdd.toIndexedSeq
    for (coords, _, _) <- finishedChunks do {
      chunksLoading -= coords
    }
    finishedChunks
  }

  private def ensurePlannerCanPlanSoon(coords: ChunkRelWorld): Boolean = {
    var readyToPlan = true
    for ch <- coords.extendedNeighbors(5) do {
      val colCoords = ch.getColumnRelWorld
      if getColumn(colCoords).isEmpty then {
        startLoadingColumnIfNeeded(colCoords)
        readyToPlan = false
      }
    }
    readyToPlan
  }

  private def chunksFinishedUnloading: IndexedSeq[ChunkRelWorld] = {
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
    val finishedChunks = chunksToRemove.toIndexedSeq
    for coords <- finishedChunks do {
      chunksUnloading -= coords
    }
    finishedChunks
  }

  private def saveColumn(columnCoords: ColumnRelWorld, col: ChunkColumnTerrain): Unit = {
    worldProvider.saveColumnData(ChunkColumnData(Some(col.terrainHeight)).toNBT, columnCoords)
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

    if e.model.isDefined then {
      e.model.get.tick(e.motion.velocity.lengthSquared() > 0.1, e.headDirection.map(_.direction))
    }
  }

  private val blockUpdateTimer: TickableTimer = TickableTimer(ServerWorld.ticksBetweenBlockUpdates)

  private def performBlockUpdates(): Seq[BlockRelWorld] = {
    val recordingWorld = RecordingBlockRepository(this)

    val blocksToUpdateNow = new ArrayBuffer[Long](blocksToUpdate.size)
    blocksToUpdate.drainInto(value => blocksToUpdateNow += value)

    Loop.array(blocksToUpdateNow) { v =>
      val c = BlockRelWorld(v)

      val block = getBlock(c).blockType
      val behaviour = block.behaviour
      if behaviour.isDefined then {
        behaviour.get.onUpdated(c, block, recordingWorld)
      }
    }

    recordingWorld.collectUpdates.distinct
  }

  private val relocateEntitiesTimer: TickableTimer = TickableTimer(ServerWorld.ticksBetweenEntityRelocation)

  private def performEntityRelocation(): Unit = {
    val entList = for {
      ch <- chunkList
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
    Loop.rangeUntil(0, 8) { side =>
      val nCoords = coords.offset(NeighborOffsets(side))
      if getChunk(nCoords).isDefined then {
        requestRenderUpdate(nCoords)
      }
    }
  }

  private def startLoadingColumnIfNeeded(here: ColumnRelWorld): Unit = {
    val columnsToLoad = mutable.ArrayBuffer.empty[ColumnRelWorld]
    if !columns.contains(here.value) && !columnsLoading.contains(here.value) then {
      columnsToLoad += here
    }
    for coords <- here.neighbors do {
      if !columns.contains(coords.value) && !columnsLoading.contains(coords.value) then {
        columnsToLoad += coords
      }
    }
    for coords <- columnsToLoad do {
      columnsLoading(coords.value) = Future(worldProvider.loadColumnData(coords))(using fsAsync).map(columnData =>
        ChunkColumnTerrain.create(
          ChunkColumnHeightMap.fromData2D(worldGenerator.getHeightmapInterpolator(coords)),
          columnData.map(ChunkColumnData.fromNbt)
        )
      )(using genAsync)
    }
  }

  private def ensureColumnExists(here: ColumnRelWorld): ChunkColumnTerrain = {
    handleLoadingColumns()

    startLoadingColumnIfNeeded(here)

    if !columns.contains(here.value) then {
      Await.ready(columnsLoading(here.value), Duration.Inf)
    }

    handleLoadingColumns()

    columns(here.value)
  }

  private def handleLoadingColumns(): Unit = {
    val completed = columnsLoading.filter(_._2.isCompleted)
    for (coords, fut) <- completed do {
      columns(coords) = fut.value.get.get
      columnsLoading -= coords
    }
  }

  def getBrightness(block: BlockRelWorld): Float = {
    getChunk(block.getChunkRelWorld) match {
      case Some(c) => c.getBrightness(block.getBlockRelChunk)
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
        backgroundTasks += Future(worldProvider.saveChunkData(chunkNbt, chunkCoords))(using fsAsync)
      }
    }

    chunks.clear()
    chunkList.clear()

    for (columnKey, column) <- columns do {
      backgroundTasks += Future(saveColumn(ColumnRelWorld(columnKey), column))(using fsAsync)
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

    fsExecutorService.shutdown()
    genExecutorService.shutdown()
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
