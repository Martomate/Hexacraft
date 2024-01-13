package hexacraft.world

import hexacraft.util.*
import hexacraft.world.block.{BlockRepository, BlockState}
import hexacraft.world.chunk.*
import hexacraft.world.coord.*
import hexacraft.world.entity.{Entity, EntityPhysicsSystem}

import com.martomate.nbt.Nbt

import scala.annotation.tailrec
import scala.collection.mutable

object World:
  private val ticksBetweenBlockUpdates = 5
  private val ticksBetweenEntityRelocation = 120

  var shouldChillChunkLoader = false

  enum Event:
    case ChunkAdded(coords: ChunkRelWorld)
    case ChunkRemoved(coords: ChunkRelWorld)
    case ChunkNeedsRenderUpdate(coords: ChunkRelWorld)

class World(worldProvider: WorldProvider, worldInfo: WorldInfo) extends BlockRepository with BlocksInWorld:
  given size: CylinderSize = worldInfo.worldSize

  private val worldGenerator = new WorldGenerator(worldInfo.gen)
  private val worldPlanner: WorldPlanner = WorldPlanner(this, worldInfo.gen.seed)
  private val lightPropagator: LightPropagator = new LightPropagator(this, this.requestRenderUpdate)

  val renderDistance: Double = 8 * CylinderSize.y60

  val collisionDetector: CollisionDetector = new CollisionDetector(this)

  private val entityPhysicsSystem = EntityPhysicsSystem(this, collisionDetector)

  private val columns = mutable.LongMap.empty[ChunkColumn]

  private val chunkLoader: ChunkLoader = makeChunkLoader()

  private val blocksToUpdate: UniqueQueue[BlockRelWorld] = new UniqueQueue

  private val savedChunkModCounts = mutable.Map.empty[ChunkRelWorld, Long]

  private val dispatcher = new EventDispatcher[World.Event]
  def trackEvents(tracker: Tracker[World.Event]): Unit = dispatcher.track(tracker)

  trackEvents(worldPlanner.onWorldEvent _)
  trackEvents(chunkLoader.onWorldEvent _)

  private def makeChunkLoader(): ChunkLoader =
    val chunkFactory = (coords: ChunkRelWorld) =>
      val (chunk, isNew) =
        worldProvider.loadChunkData(coords) match
          case Some(loadedTag) => (Chunk.fromNbt(coords, loadedTag), false)
          case None            => (Chunk.fromGenerator(coords, this, worldGenerator), true)
      savedChunkModCounts(coords) = if isNew then -1L else chunk.modCount
      chunk

    val chunkUnloader = (coords: ChunkRelWorld) =>
      for chunk <- getChunk(coords) do
        if chunk.modCount != savedChunkModCounts.getOrElse(coords, -1L) then
          worldProvider.saveChunkData(chunk.toNbt, chunk.coords)
        savedChunkModCounts -= coords

    new ChunkLoader(chunkFactory, chunkUnloader, renderDistance)

  def getColumn(coords: ColumnRelWorld): Option[ChunkColumnTerrain] = columns.get(coords.value)

  def getChunk(coords: ChunkRelWorld): Option[Chunk] =
    columns.get(coords.getColumnRelWorld.value).flatMap(_.getChunk(coords.Y))

  def getBlock(coords: BlockRelWorld): BlockState =
    getChunk(coords.getChunkRelWorld) match
      case Some(chunk) => chunk.getBlock(coords.getBlockRelChunk)
      case None        => BlockState.Air

  def provideColumn(coords: ColumnRelWorld): ChunkColumnTerrain = ensureColumnExists(coords)

  def setBlock(coords: BlockRelWorld, block: BlockState): Unit =
    getChunk(coords.getChunkRelWorld) match
      case Some(chunk) =>
        chunk.setBlock(coords.getBlockRelChunk, block)
        onSetBlock(coords, block)
      case None =>

  def removeBlock(coords: BlockRelWorld): Unit =
    getChunk(coords.getChunkRelWorld) match
      case Some(chunk) =>
        chunk.setBlock(coords.getBlockRelChunk, BlockState.Air)
        onSetBlock(coords, BlockState.Air)
      case None =>

  def addEntity(entity: Entity): Unit =
    chunkOfEntity(entity) match
      case Some(chunk) => chunk.addEntity(entity)
      case None        =>

  def removeEntity(entity: Entity): Unit =
    chunkOfEntity(entity) match
      case Some(chunk) => chunk.removeEntity(entity)
      case None        =>

  def removeAllEntities(): Unit =
    for
      col <- columns.values
      ch <- col.allChunks
      e <- ch.entities.toSeq
    do ch.removeEntity(e)

  private def chunkOfEntity(entity: Entity): Option[Chunk] =
    getChunk(CoordUtils.approximateChunkCoords(entity.transform.position))

  def getHeight(x: Int, z: Int): Int =
    val coords = ColumnRelWorld(x >> 4, z >> 4)
    ensureColumnExists(coords).terrainHeight(x & 15, z & 15)

  def setChunk(ch: Chunk): Unit =
    val col = ensureColumnExists(ch.coords.getColumnRelWorld)
    col.setChunk(ch)

    dispatcher.notify(World.Event.ChunkAdded(ch.coords))

    worldPlanner.decorate(ch)
    if ch.modCount != savedChunkModCounts.getOrElse(ch.coords, -1L) then
      worldProvider.saveChunkData(ch.toNbt, ch.coords)
      savedChunkModCounts(ch.coords) = ch.modCount
      col.updateHeightmapAfterChunkUpdate(ch)

    ch.initLightingIfNeeded(lightPropagator)

    requestRenderUpdate(ch.coords)
    requestRenderUpdateForNeighborChunks(ch.coords)

    for block <- ch.blocks do
      requestBlockUpdate(BlockRelWorld.fromChunk(block.coords, ch.coords))

      for side <- 0 until 8 do
        if block.coords.isOnChunkEdge(side) then
          val neighCoords = block.coords.neighbor(side)
          for neighbor <- getChunk(ch.coords.offset(ChunkRelWorld.neighborOffsets(side))) do
            requestBlockUpdate(BlockRelWorld.fromChunk(neighCoords, neighbor.coords))

  def removeChunk(ch: ChunkRelWorld): Unit =
    for col <- columns.get(ch.getColumnRelWorld.value) do
      for removedChunk <- col.removeChunk(ch.Y) do
        dispatcher.notify(World.Event.ChunkRemoved(removedChunk.coords))

        if removedChunk.modCount != savedChunkModCounts.getOrElse(removedChunk.coords, -1L) then
          worldProvider.saveChunkData(removedChunk.toNbt, removedChunk.coords)
          savedChunkModCounts -= removedChunk.coords
        requestRenderUpdateForNeighborChunks(ch)

      if col.isEmpty then
        columns.remove(col.coords.value)
        worldProvider.saveColumnData(col.toNBT, col.coords)

  def tick(camera: Camera): Unit =
    val (chunksToAdd, chunksToRemove) = chunkLoader.tick(
      PosAndDir.fromCameraView(camera.view),
      World.shouldChillChunkLoader
    )

    for ch <- chunksToAdd do setChunk(ch)
    for ch <- chunksToRemove do removeChunk(ch)

    if blockUpdateTimer.tick() then performBlockUpdates()
    if relocateEntitiesTimer.tick() then performEntityRelocation()

    for
      col <- columns.values
      ch <- col.allChunks
    do
      ch.optimizeStorage()
      tickEntities(ch.entities)

  @tailrec // this is done for performance
  private def tickEntities(ents: Iterable[Entity]): Unit =
    if ents.nonEmpty then
      tickEntity(ents.head)
      tickEntities(ents.tail)

  private def tickEntity(e: Entity): Unit =
    e.ai.foreach: ai =>
      ai.tick(this, e.transform, e.velocity, e.boundingBox)
      e.velocity.velocity.add(ai.acceleration())

    e.velocity.velocity.x *= 0.9
    e.velocity.velocity.z *= 0.9

    entityPhysicsSystem.update(e.transform, e.velocity, e.boundingBox)

    e.model.foreach(_.tick())

  private val blockUpdateTimer: TickableTimer = TickableTimer(World.ticksBetweenBlockUpdates)

  private def performBlockUpdates(): Unit =
    val blocksToUpdateLen = blocksToUpdate.size
    for _ <- 0 until blocksToUpdateLen do
      val c = blocksToUpdate.dequeue()
      val block = getBlock(c).blockType
      block.behaviour.foreach(_.onUpdated(c, block, this))

  private val relocateEntitiesTimer: TickableTimer = TickableTimer(World.ticksBetweenEntityRelocation)

  private def performEntityRelocation(): Unit =
    val entList = for
      col <- columns.values
      ch <- col.allChunks
      ent <- ch.entities
    yield (ch, ent, chunkOfEntity(ent))

    for
      (ch, ent, newOpt) <- entList
      newChunk <- newOpt
      if newChunk != ch
    do
      ch.removeEntity(ent)
      newChunk.addEntity(ent)

  private def requestRenderUpdateForNeighborChunks(coords: ChunkRelWorld): Unit =
    for
      side <- 0 until 8
      ch <- getChunk(coords.offset(NeighborOffsets(side)))
    do requestRenderUpdate(ch.coords)

  private def ensureColumnExists(here: ColumnRelWorld): ChunkColumn =
    columns.get(here.value) match
      case Some(col) => col
      case None =>
        val col = ChunkColumn.create(
          here,
          ChunkColumnHeightMap.fromData2D(worldGenerator.getHeightmapInterpolator(here)),
          worldProvider.loadColumnData(here).map(ChunkColumnData.fromNbt)
        )
        columns(here.value) = col
        col

  def getBrightness(block: BlockRelWorld): Float =
    getChunk(block.getChunkRelWorld) match
      case Some(c) => c.lighting.getBrightness(block.getBlockRelChunk)
      case None    => 1.0f

  def onReloadedResources(): Unit =
    for
      col <- columns.values
      ch <- col.allChunks
    do requestRenderUpdate(ch.coords)

  private def requestRenderUpdate(chunkCoords: ChunkRelWorld): Unit =
    dispatcher.notify(World.Event.ChunkNeedsRenderUpdate(chunkCoords))

  def unload(): Unit =
    blockUpdateTimer.enabled = false
    for col <- columns.values do
      for ch <- col.allChunks if ch.modCount != savedChunkModCounts.getOrElse(ch.coords, -1L) do
        worldProvider.saveChunkData(ch.toNbt, ch.coords)

      worldProvider.saveColumnData(col.toNBT, col.coords)
    columns.clear()
    chunkLoader.unload()

  private def requestBlockUpdate(coords: BlockRelWorld): Unit = blocksToUpdate.enqueue(coords)

  private def onSetBlock(coords: BlockRelWorld, block: BlockState): Unit =
    def affectedChunkOffset(where: Byte): Int = where match
      case 0  => -1
      case 15 => 1
      case _  => 0

    def isInNeighborChunk(chunkOffset: Offset) =
      val xx = affectedChunkOffset(coords.cx)
      val yy = affectedChunkOffset(coords.cy)
      val zz = affectedChunkOffset(coords.cz)

      chunkOffset.dx * xx == 1 || chunkOffset.dy * yy == 1 || chunkOffset.dz * zz == 1

    for col <- columns.get(coords.getColumnRelWorld.value) do col.updateHeightmapAfterBlockUpdate(coords, block)

    val cCoords = coords.getChunkRelWorld
    val bCoords = coords.getBlockRelChunk

    for c <- getChunk(cCoords) do
      handleLightingOnSetBlock(c, bCoords, block)

      requestRenderUpdate(c.coords)
      requestBlockUpdate(BlockRelWorld.fromChunk(bCoords, c.coords))

      for i <- 0 until 8 do
        val off = ChunkRelWorld.neighborOffsets(i)
        val c2 = bCoords.offset(off)

        if isInNeighborChunk(off) then
          for n <- getChunk(cCoords.offset(NeighborOffsets(i))) do
            requestRenderUpdate(n.coords)
            requestBlockUpdate(BlockRelWorld.fromChunk(c2, n.coords))
        else requestBlockUpdate(BlockRelWorld.fromChunk(c2, c.coords))

  private def handleLightingOnSetBlock(chunk: Chunk, blockCoords: BlockRelChunk, block: BlockState): Unit =
    lightPropagator.removeTorchlight(chunk, blockCoords)
    lightPropagator.removeSunlight(chunk, blockCoords)
    if block.blockType.lightEmitted != 0 then
      lightPropagator.addTorchlight(chunk, blockCoords, block.blockType.lightEmitted)
