package hexacraft.world.chunk

import hexacraft.util.{EventDispatcher, RevokeTrackerFn, SmartArray, Tracker}
import hexacraft.util.Result.{Err, Ok}
import hexacraft.world.{BlocksInWorld, CollisionDetector, CylinderSize, LightPropagator, WorldGenerator}
import hexacraft.world.block.BlockState
import hexacraft.world.coord.{BlockRelChunk, BlockRelWorld, ChunkRelWorld}
import hexacraft.world.entity.{Entity, EntityRegistry}

import com.martomate.nbt.Nbt

import scala.annotation.tailrec
import scala.collection.mutable

object Chunk:
  enum Event:
    case ChunkNeedsRenderUpdate(coords: ChunkRelWorld)
    case BlockReplaced(coords: BlockRelWorld, prev: BlockState, now: BlockState)

  def fromNbt(coords: ChunkRelWorld, loadedTag: Nbt.MapTag, entityRegistry: EntityRegistry)(using CylinderSize): Chunk =
    new Chunk(coords, ChunkData.fromNBT(loadedTag)(entityRegistry), false)

  def fromGenerator(coords: ChunkRelWorld, world: BlocksInWorld, generator: WorldGenerator)(using CylinderSize) =
    val column = world.provideColumn(coords.getColumnRelWorld)
    new Chunk(coords, generator.generateChunk(coords, column), true)

class Chunk private (val coords: ChunkRelWorld, chunkData: ChunkData, initNeedsToSave: Boolean)(using CylinderSize):
  private var needsToSave: Boolean = initNeedsToSave

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

  private def save(): Nbt.MapTag =
    val chunkTag = chunkData.toNBT
    needsToSave = false
    chunkTag

  def isDecorated: Boolean = chunkData.isDecorated
  def setDecorated(): Unit =
    if !chunkData.isDecorated then
      chunkData.setDecorated()
      needsToSave = true

class ChunkData(private var storage: ChunkStorage, entities: mutable.ArrayBuffer[Entity])(using CylinderSize):
  private var _isDecorated: Boolean = false
  def isDecorated: Boolean = _isDecorated

  def optimizeStorage(): Unit =
    if storage.isDense then {
      if storage.numBlocks < 32
      then storage = SparseChunkStorage.fromStorage(storage)
    } else {
      if storage.numBlocks > 48
      then storage = DenseChunkStorage.fromStorage(storage)
    }

  def getBlock(coords: BlockRelChunk): BlockState = storage.getBlock(coords)
  def setBlock(coords: BlockRelChunk, block: BlockState): Unit = storage.setBlock(coords, block)
  def allBlocks: Array[LocalBlockState] = storage.allBlocks

  def addEntity(entity: Entity): Unit = entities += entity
  def removeEntity(entity: Entity): Unit = entities -= entity
  def allEntities: collection.Seq[Entity] = entities

  def setDecorated(): Unit = _isDecorated = true

  def toNBT: Nbt.MapTag =
    val storageNbt = storage.toNBT

    Nbt.makeMap(
      "blocks" -> Nbt.ByteArrayTag(storageNbt.blocks),
      "metadata" -> Nbt.ByteArrayTag(storageNbt.metadata),
      "entities" -> Nbt.ListTag(allEntities.map(e => e.toNBT).toSeq),
      "isDecorated" -> Nbt.ByteTag(_isDecorated)
    )

object ChunkData:
  def fromStorage(storage: ChunkStorage)(using CylinderSize): ChunkData =
    new ChunkData(storage, mutable.ArrayBuffer.empty)

  def fromNBT(nbt: Nbt.MapTag)(registry: EntityRegistry)(using CylinderSize): ChunkData =
    val storage = nbt.getByteArray("blocks") match
      case Some(blocks) =>
        val meta = nbt.getByteArray("metadata")
        DenseChunkStorage.fromNBT(blocks.toArray, meta.map(_.toArray))
      case None =>
        SparseChunkStorage.empty

    val entities =
      nbt.getList("entities") match
        case Some(tags) => entitiesFromNbt(tags.map(_.asInstanceOf[Nbt.MapTag]), registry)
        case None       => Nil

    val data = new ChunkData(storage, mutable.ArrayBuffer.from(entities))
    data._isDecorated = nbt.getBoolean("isDecorated", default = false)
    data

  private def entitiesFromNbt(list: Seq[Nbt.MapTag], registry: EntityRegistry)(using
      CylinderSize
  ): Iterable[Entity] =
    val entities = mutable.ArrayBuffer.empty[Entity]

    for tag <- list do
      Entity.fromNbt(tag, registry) match
        case Ok(entity)   => entities += entity
        case Err(message) => println(message)

    entities

class ChunkLighting:
  private val brightness: SmartArray[Byte] = SmartArray.withByteArray(16 * 16 * 16, 0)
  private var brightnessInitialized: Boolean = false

  def initialized: Boolean = brightnessInitialized

  def setInitialized(): Unit = brightnessInitialized = true

  def setSunlight(coords: BlockRelChunk, value: Int): Unit =
    brightness(coords.value) = (brightness(coords.value) & 0xf | value << 4).toByte

  def getSunlight(coords: BlockRelChunk): Byte =
    ((brightness(coords.value) >> 4) & 0xf).toByte

  def setTorchlight(coords: BlockRelChunk, value: Int): Unit =
    brightness(coords.value) = (brightness(coords.value) & 0xf0 | value).toByte

  def getTorchlight(coords: BlockRelChunk): Byte =
    (brightness(coords.value) & 0xf).toByte

  def getBrightness(block: BlockRelChunk): Float =
    math.min((getTorchlight(block) + getSunlight(block)) / 15f, 1.0f)
