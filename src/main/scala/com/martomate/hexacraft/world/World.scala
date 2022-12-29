package com.martomate.hexacraft.world

import com.martomate.hexacraft.util.*
import com.martomate.hexacraft.world.block.{Blocks, BlockSetAndGet, BlockState}
import com.martomate.hexacraft.world.camera.Camera
import com.martomate.hexacraft.world.chunk.*
import com.martomate.hexacraft.world.coord.CoordUtils
import com.martomate.hexacraft.world.coord.fp.{BlockCoords, CylCoords}
import com.martomate.hexacraft.world.coord.integer.*
import com.martomate.hexacraft.world.entity.{Entity, EntityModelLoader, EntityRegistry}
import com.martomate.hexacraft.world.entity.player.PlayerFactory
import com.martomate.hexacraft.world.entity.sheep.SheepFactory
import com.martomate.hexacraft.world.gen.{WorldGenerator, WorldPlanner}
import com.martomate.hexacraft.world.loader.{ChunkLoader, ChunkLoaderDistPQ, PosAndDir}
import com.martomate.hexacraft.world.player.Player
import com.martomate.hexacraft.world.settings.WorldInfo

import com.flowpowered.nbt.{ByteTag, CompoundTag, ShortTag, StringTag}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object World:
  private val ticksBetweenBlockUpdates = 5
  private val ticksBetweenEntityRelocation = 120

  var shouldChillChunkLoader = false

  def apply(provider: WorldProvider)(using Blocks): World =
    val worldInfo = provider.getWorldInfo
    new World(provider, worldInfo, makeEntityRegistry())

  private def makeEntityRegistry(): EntityRegistry =
    given EntityModelLoader = new EntityModelLoader()

    val entityTypes = Map(
      "player" -> new PlayerFactory,
      "sheep" -> new SheepFactory
    )
    EntityRegistry.from(entityTypes)

class World(worldProvider: WorldProvider, worldInfo: WorldInfo, val entityRegistry: EntityRegistry)(using Blocks)
    extends BlockSetAndGet
    with BlocksInWorld
    with ChunkColumnListener:
  val size: CylinderSize = worldInfo.worldSize
  import size.impl

  private val worldGenerator = new WorldGenerator(worldInfo.gen)
  private val worldPlanner: WorldPlanner = WorldPlanner(this, entityRegistry, worldInfo.gen.seed)
  private val lightPropagator: LightPropagator = new LightPropagator(this)

  val renderDistance: Double = 8 * CylinderSize.y60

  val collisionDetector: CollisionDetector = new CollisionDetector(this)

  private val columns = mutable.LongMap.empty[ChunkColumn]

  private val chunkLoadingOrigin = new PosAndDir
  private val chunkLoader: ChunkLoader = makeChunkLoader()

  private val blocksToUpdate: UniqueQueue[BlockRelWorld] = new UniqueQueue

  val player: Player = makePlayer

  private val chunkAddedOrRemovedListeners: ArrayBuffer[ChunkAddedOrRemovedListener] =
    ArrayBuffer.empty
  def addChunkAddedOrRemovedListener(listener: ChunkAddedOrRemovedListener): Unit =
    chunkAddedOrRemovedListeners += listener
  def removeChunkAddedOrRemovedListener(listener: ChunkAddedOrRemovedListener): Unit =
    chunkAddedOrRemovedListeners -= listener

  addChunkAddedOrRemovedListener(worldPlanner)
  addChunkAddedOrRemovedListener(chunkLoader)

  saveWorldData()

  private def makeChunkLoader(): ChunkLoader =
    val chunkFactory = (coords: ChunkRelWorld) =>
      val generator = new ChunkGenerator(coords, this, worldProvider, worldGenerator, entityRegistry)
      new Chunk(coords, generator, lightPropagator)

    val chunkUnloader = (coords: ChunkRelWorld) => getChunk(coords).foreach(_.saveIfNeeded())

    new ChunkLoaderDistPQ(
      chunkLoadingOrigin,
      chunkFactory,
      chunkUnloader,
      renderDistance,
      () => World.shouldChillChunkLoader
    )

  def onBlockNeedsUpdate(coords: BlockRelWorld): Unit = blocksToUpdate.enqueue(coords)

  def onChunkNeedsRenderUpdate(coords: ChunkRelWorld): Unit = ()

  def getColumn(coords: ColumnRelWorld): Option[ChunkColumn] = columns.get(coords.value)

  def getChunk(coords: ChunkRelWorld): Option[Chunk] =
    getColumn(coords.getColumnRelWorld).flatMap(_.getChunk(coords.getChunkRelColumn))

  def getBlock(coords: BlockRelWorld): BlockState =
    getChunk(coords.getChunkRelWorld)
      .map(_.getBlock(coords.getBlockRelChunk))
      .getOrElse(BlockState.Air)

  def provideColumn(coords: ColumnRelWorld): ChunkColumn = ensureColumnExists(coords)

  def setBlock(coords: BlockRelWorld, block: BlockState): Unit =
    getChunk(coords.getChunkRelWorld).foreach(_.setBlock(coords.getBlockRelChunk, block))

  def removeBlock(coords: BlockRelWorld): Unit =
    getChunk(coords.getChunkRelWorld).foreach(_.removeBlock(coords.getBlockRelChunk))

  def addEntity(entity: Entity): Unit =
    chunkOfEntity(entity).foreach(_.entities += entity)

  def removeEntity(entity: Entity): Unit =
    chunkOfEntity(entity).foreach(_.entities -= entity)

  def removeAllEntities(): Unit =
    for
      col <- columns.values
      ch <- col.allChunks
      e <- ch.entities.allEntities.toSeq
    do ch.entities -= e

  private def chunkOfEntity(entity: Entity): Option[Chunk] =
    getApproximateChunk(entity.position)

  private def getApproximateChunk(coords: CylCoords): Option[Chunk] =
    getChunk(CoordUtils.approximateChunkCoords(coords))

  private def getHeight(x: Int, z: Int): Int =
    val coords = ColumnRelWorld(x >> 4, z >> 4)
    ensureColumnExists(coords).heightMap(x & 15, z & 15)

  def setChunk(ch: Chunk): Unit =
    ensureColumnExists(ch.coords.getColumnRelWorld).setChunk(ch)
    chunkAddedOrRemovedListeners.foreach(_.onChunkAdded(ch))
    ch.init()
    worldPlanner.decorate(ch)

    for block <- ch.blocks.allBlocks do
      ch.requestBlockUpdate(block.coords)

      for side <- 0 until 8 do
        if block.coords.onChunkEdge(side) then
          val neighCoords = block.coords.neighbor(side)
          getChunk(ch.coords.offset(ChunkRelWorld.neighborOffsets(side)))
            .foreach(_.requestBlockUpdate(neighCoords))

  def removeChunk(ch: ChunkRelWorld): Unit =
    getColumn(ch.getColumnRelWorld) match
      case Some(col) =>
        col.removeChunk(ch.getChunkRelColumn) match
          case Some(removedChunk) =>
            for l <- chunkAddedOrRemovedListeners do l.onChunkRemoved(removedChunk)
            removedChunk.unload()
          case None =>

        if col.isEmpty then
          columns.remove(col.coords.value) match
            case Some(removedColumn) => // should be the same as `col`
              removedColumn.removeEventListener(this)
              removedColumn.unload()
            case None =>
      case None =>

  def tick(camera: Camera): Unit =
    chunkLoadingOrigin.setPosAndDirFrom(camera.view)
    chunkLoader.tick()

    for ch <- chunkLoader.chunksToAdd() do setChunk(ch)
    for ch <- chunkLoader.chunksToRemove() do removeChunk(ch)

    if blockUpdateTimer.tick() then performBlockUpdates()
    if relocateEntitiesTimer.tick() then performEntityRelocation()

    for col <- columns.values do col.tick(this, collisionDetector)

  private val blockUpdateTimer: TickableTimer = TickableTimer(World.ticksBetweenBlockUpdates)

  private def performBlockUpdates(): Unit =
    val blocksToUpdateLen = blocksToUpdate.size
    for _ <- 0 until blocksToUpdateLen do
      val c = blocksToUpdate.dequeue()
      getBlock(c).blockType.doUpdate(c, this)

  private val relocateEntitiesTimer: TickableTimer = TickableTimer(World.ticksBetweenEntityRelocation)

  private def performEntityRelocation(): Unit =
    val entList = for
      col <- columns.values
      ch <- col.allChunks
      ent <- ch.entities.allEntities
    yield (ch, ent, chunkOfEntity(ent))

    for (ch, ent, newOpt) <- entList do
      newOpt match
        case Some(newChunk) =>
          if newChunk != ch then
            ch.entities -= ent
            newChunk.entities += ent
        case None =>

  private def ensureColumnExists(here: ColumnRelWorld): ChunkColumn =
    columns.get(here.value) match
      case Some(col) => col
      case None =>
        val col = new ChunkColumn(here, worldGenerator, worldProvider)
        columns(here.value) = col
        col.addEventListener(this)
        col

  def getBrightness(block: BlockRelWorld): Float =
    getChunk(block.getChunkRelWorld) match
      case Some(c) => c.lighting.getBrightness(block.getBlockRelChunk)
      case None    => 1.0f

  def onReloadedResources(): Unit = columns.values.foreach(_.onReloadedResources())

  def unload(): Unit =
    saveWorldData()

    chunkLoader.unload()
    blockUpdateTimer.active = false
    columns.values.foreach(_.unload())
    columns.clear()
    chunkAddedOrRemovedListeners.clear()

  private def saveWorldData(): Unit =
    val worldTag = new WorldInfo(worldInfo.worldName, worldInfo.worldSize, worldInfo.gen, player.toNBT).toNBT

    worldProvider.saveState(worldTag, "world.dat")

  override def onChunksNeighborNeedsRenderUpdate(coords: ChunkRelWorld, side: Int): Unit =
    getChunk(coords.offset(NeighborOffsets(side))) match
      case Some(ch) => ch.requestRenderUpdate()
      case None     =>

  override def onSetBlock(coords: BlockRelWorld, prev: BlockState, now: BlockState): Unit =
    def affectedChunkOffset(where: Byte): Int = where match
      case 0  => -1
      case 15 => 1
      case _  => 0

    def isInNeighborChunk(chunkOffset: Offset) =
      val xx = affectedChunkOffset(coords.cx)
      val yy = affectedChunkOffset(coords.cy)
      val zz = affectedChunkOffset(coords.cz)

      chunkOffset.dx * xx == 1 || chunkOffset.dy * yy == 1 || chunkOffset.dz * zz == 1

    val cCoords = coords.getChunkRelWorld
    val bCoords = coords.getBlockRelChunk

    getChunk(cCoords) match
      case Some(c) =>
        c.requestRenderUpdate()
        c.requestBlockUpdate(bCoords)

        for i <- 0 until 8 do
          val off = ChunkRelWorld.neighborOffsets(i)
          val c2 = bCoords.offset(off)

          if isInNeighborChunk(off) then
            getChunk(cCoords.offset(NeighborOffsets(i))) match
              case Some(n) =>
                n.requestRenderUpdate()
                n.requestBlockUpdate(c2)
              case None =>
          else c.requestBlockUpdate(c2)
      case None =>

  private def makePlayer: Player =
    if worldInfo.player != null then Player.fromNBT(worldInfo.player)
    else
      val startX = (math.random() * 100 - 50).toInt
      val startZ = (math.random() * 100 - 50).toInt
      val startY = getHeight(startX, startZ) + 4
      Player.atStartPos(BlockCoords(startX, startY, startZ).toCylCoords)
