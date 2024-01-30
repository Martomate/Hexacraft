package hexacraft.world

import hexacraft.math.bits.Int12
import hexacraft.util.*
import hexacraft.world.block.{Block, BlockRepository, BlockState}
import hexacraft.world.chunk.*
import hexacraft.world.coord.*
import hexacraft.world.entity.{Entity, EntityPhysicsSystem}

import com.martomate.nbt.Nbt

import scala.annotation.tailrec
import scala.collection.mutable

object World {
  private val ticksBetweenBlockUpdates = 5
  private val ticksBetweenEntityRelocation = 120

  var shouldChillChunkLoader = false

  enum Event {
    case ChunkAdded(coords: ChunkRelWorld)
    case ChunkRemoved(coords: ChunkRelWorld)
    case ChunkNeedsRenderUpdate(coords: ChunkRelWorld)
  }
}

class World(worldProvider: WorldProvider, worldInfo: WorldInfo) extends BlockRepository with BlocksInWorld {
  given size: CylinderSize = worldInfo.worldSize

  private val worldGenerator = new WorldGenerator(worldInfo.gen)
  private val worldPlanner: WorldPlanner = WorldPlanner(this, worldInfo.gen.seed)
  private val lightPropagator: LightPropagator = new LightPropagator(this, this.requestRenderUpdate)

  val renderDistance: Double = 8 * CylinderSize.y60

  val collisionDetector: CollisionDetector = new CollisionDetector(this)

  private val entityPhysicsSystem = EntityPhysicsSystem(this, collisionDetector)

  private val columns: mutable.LongMap[ChunkColumnTerrain] = mutable.LongMap.empty
  private val chunks: mutable.LongMap[Chunk] = mutable.LongMap.empty

  private val chunkLoader: ChunkLoader = makeChunkLoader()

  private val blocksToUpdate: UniqueQueue[BlockRelWorld] = new UniqueQueue

  private val savedChunkModCounts = mutable.Map.empty[ChunkRelWorld, Long]

  private val dispatcher = new EventDispatcher[World.Event]

  def trackEvents(tracker: Tracker[World.Event]): Unit = {
    dispatcher.track(tracker)
  }

  trackEvents(worldPlanner.onWorldEvent _)
  trackEvents(chunkLoader.onWorldEvent _)

  private def makeChunkLoader(): ChunkLoader = {
    val chunkFactory = (coords: ChunkRelWorld) => {
      val (chunk, isNew) =
        worldProvider.loadChunkData(coords) match {
          case Some(loadedTag) => (Chunk.fromNbt(loadedTag), false)
          case None            => (Chunk.fromGenerator(coords, this, worldGenerator), true)
        }
      savedChunkModCounts(coords) = if isNew then -1L else chunk.modCount
      chunk
    }

    val chunkUnloader = (coords: ChunkRelWorld) => {
      for chunk <- getChunk(coords) do {
        if chunk.modCount != savedChunkModCounts.getOrElse(coords, -1L) then {
          worldProvider.saveChunkData(chunk.toNbt, coords)
        }
        savedChunkModCounts -= coords
      }
    }

    new ChunkLoader(chunkFactory, chunkUnloader, renderDistance)
  }

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

    dispatcher.notify(World.Event.ChunkAdded(chunkCoords))

    worldPlanner.decorate(chunkCoords, ch)
    if ch.modCount != savedChunkModCounts.getOrElse(chunkCoords, -1L) then {
      worldProvider.saveChunkData(ch.toNbt, chunkCoords)
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

  def removeChunk(chunkCoords: ChunkRelWorld): Unit = {
    val columnCoords = chunkCoords.getColumnRelWorld

    for col <- columns.get(columnCoords.value) do {
      for removedChunk <- chunks.remove(chunkCoords.value) do {
        dispatcher.notify(World.Event.ChunkRemoved(chunkCoords))

        if removedChunk.modCount != savedChunkModCounts.getOrElse(chunkCoords, -1L) then {
          worldProvider.saveChunkData(removedChunk.toNbt, chunkCoords)
          savedChunkModCounts -= chunkCoords
        }
        requestRenderUpdateForNeighborChunks(chunkCoords)
      }

      if chunks.keys.count(v => ChunkRelWorld(v).getColumnRelWorld == columnCoords) == 0 then {
        columns.remove(columnCoords.value)
        saveColumn(columnCoords, col)
      }
    }
  }

  def tick(camera: Camera): Unit = {
    val (chunksToAdd, chunksToRemove) =
      chunkLoader.tick(PosAndDir.fromCameraView(camera.view), World.shouldChillChunkLoader)

    for (chunkCoords, chunk) <- chunksToAdd do {
      setChunk(chunkCoords, chunk)
    }
    for chunkCoords <- chunksToRemove do {
      removeChunk(chunkCoords)
    }

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

    e.model.foreach(_.tick())
  }

  private val blockUpdateTimer: TickableTimer = TickableTimer(World.ticksBetweenBlockUpdates)

  private def performBlockUpdates(): Unit = {
    val blocksToUpdateLen = blocksToUpdate.size
    for _ <- 0 until blocksToUpdateLen do {
      val c = blocksToUpdate.dequeue()
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
    dispatcher.notify(World.Event.ChunkNeedsRenderUpdate(chunkCoords))
  }

  def unload(): Unit = {
    blockUpdateTimer.enabled = false

    for (chunkKey, chunk) <- chunks do {
      val chunkCoords = ChunkRelWorld(chunkKey)

      if chunk.modCount != savedChunkModCounts.getOrElse(chunkCoords, -1L) then {
        worldProvider.saveChunkData(chunk.toNbt, chunkCoords)
      }
    }

    for (columnKey, column) <- columns do {
      saveColumn(ColumnRelWorld(columnKey), column)
    }

    chunks.clear()
    columns.clear()
    chunkLoader.unload()
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
      requestBlockUpdate(BlockRelWorld.fromChunk(bCoords, cCoords))

      for i <- 0 until 8 do {
        val off = ChunkRelWorld.neighborOffsets(i)
        val c2 = bCoords.offset(off)

        if isInNeighborChunk(off) then {
          val nCoords = cCoords.offset(NeighborOffsets(i))
          for n <- getChunk(nCoords) do {
            requestRenderUpdate(nCoords)
            requestBlockUpdate(BlockRelWorld.fromChunk(c2, nCoords))
          }
        } else {
          requestBlockUpdate(BlockRelWorld.fromChunk(c2, cCoords))
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
