package hexacraft.world.chunk

import hexacraft.nbt.Nbt
import hexacraft.util.{EventDispatcher, RevokeTrackerFn, Tracker}
import hexacraft.world.*
import hexacraft.world.block.BlockState
import hexacraft.world.chunk.storage.LocalBlockState
import hexacraft.world.coord.integer.{BlockRelChunk, BlockRelWorld, ChunkRelWorld}
import hexacraft.world.entity.{Entity, EntityRegistry}
import hexacraft.world.gen.WorldGenerator

import scala.annotation.tailrec

object Chunk:
  def apply(
      coords: ChunkRelWorld,
      world: BlocksInWorld,
      worldProvider: WorldProvider,
      entityRegistry: EntityRegistry = EntityRegistry.empty
  )(using CylinderSize): Chunk =
    val worldGenerator = new WorldGenerator(worldProvider.getWorldInfo.gen)
    val chunkGenerator = new ChunkGenerator(coords, world, worldGenerator)
    new Chunk(coords, chunkGenerator, worldProvider, entityRegistry)

  enum Event:
    case ChunkNeedsRenderUpdate(coords: ChunkRelWorld)
    case BlockReplaced(coords: BlockRelWorld, prev: BlockState, now: BlockState)

class Chunk(
    val coords: ChunkRelWorld,
    generator: ChunkGenerator,
    worldProvider: WorldProvider,
    registry: EntityRegistry
)(using CylinderSize):
  private val loadedTag: Nbt.MapTag = worldProvider.loadChunkData(coords)
  private val chunkData: ChunkData =
    if loadedTag.vs.nonEmpty then ChunkData.fromNBT(loadedTag)(registry) else generator.generate()
  private var needsToSave: Boolean = loadedTag.vs.isEmpty

  private val dispatcher = new EventDispatcher[Chunk.Event]
  def trackEvents(tracker: Tracker[Chunk.Event]): RevokeTrackerFn = dispatcher.track(tracker)

  val lighting: ChunkLighting = new ChunkLighting
  def initLightingIfNeeded(lightPropagator: LightPropagator): Unit =
    if !lighting.initialized then
      lighting.setInitialized()
      lightPropagator.initBrightnesses(this)

  def entities: collection.Seq[Entity] = chunkData.allEntities

  def addEntity(entity: Entity): Unit =
    chunkData.addEntity(entity)
    needsToSave = true

  def removeEntity(entity: Entity): Unit =
    chunkData.removeEntity(entity)
    needsToSave = true

  def blocks: Array[LocalBlockState] = chunkData.allBlocks

  def getBlock(coords: BlockRelChunk): BlockState = chunkData.getBlock(coords)

  def setBlock(blockCoords: BlockRelChunk, block: BlockState): Unit =
    val before = getBlock(blockCoords)
    if before != block then
      chunkData.setBlock(blockCoords, block)
      needsToSave = true

      dispatcher.notify(Chunk.Event.BlockReplaced(BlockRelWorld.fromChunk(blockCoords, coords), before, block))

  def removeBlock(coords: BlockRelChunk): Unit = setBlock(coords, BlockState.Air)

  def requestRenderUpdate(): Unit =
    dispatcher.notify(Chunk.Event.ChunkNeedsRenderUpdate(coords))

  def tick(world: BlocksInWorld, collisionDetector: CollisionDetector): Unit =
    chunkData.optimizeStorage()

    tickEntities(entities)

    @tailrec
    def tickEntities(ents: Iterable[Entity]): Unit =
      if ents.nonEmpty then
        ents.head.tick(world, collisionDetector)
        tickEntities(ents.tail)

  def saveIfNeeded(): Option[Nbt.MapTag] = if needsToSave then Some(save()) else None

  def unload(): Unit =
    saveIfNeeded().foreach(data => worldProvider.saveChunkData(data, coords))

  private def save(): Nbt.MapTag =
    val chunkTag = chunkData.toNBT
    needsToSave = false
    chunkTag

  def isDecorated: Boolean = chunkData.isDecorated
  def setDecorated(): Unit =
    if !chunkData.isDecorated then
      chunkData.setDecorated()
      needsToSave = true
