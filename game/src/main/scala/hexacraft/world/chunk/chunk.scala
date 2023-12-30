package hexacraft.world.chunk

import hexacraft.util.Result.{Err, Ok}
import hexacraft.util.SmartArray
import hexacraft.world.*
import hexacraft.world.block.BlockState
import hexacraft.world.coord.{BlockRelChunk, ChunkRelWorld}
import hexacraft.world.entity.{Entity, EntityFactory}

import com.martomate.nbt.Nbt

import scala.collection.mutable

object Chunk:
  def fromNbt(coords: ChunkRelWorld, loadedTag: Nbt.MapTag)(using CylinderSize): Chunk =
    new Chunk(coords, ChunkData.fromNBT(loadedTag))

  def fromGenerator(coords: ChunkRelWorld, world: BlocksInWorld, generator: WorldGenerator)(using CylinderSize) =
    val column = world.provideColumn(coords.getColumnRelWorld)
    new Chunk(coords, generator.generateChunk(coords, column))

class Chunk private (val coords: ChunkRelWorld, chunkData: ChunkData)(using CylinderSize):
  private var _modCount: Long = 0L
  def modCount: Long = _modCount

  val lighting: ChunkLighting = new ChunkLighting
  def initLightingIfNeeded(lightPropagator: LightPropagator): Unit =
    if !lighting.initialized then
      lighting.setInitialized()
      lightPropagator.initBrightnesses(this)

  def entities: collection.Seq[Entity] = chunkData.entities

  def addEntity(entity: Entity): Unit =
    chunkData.entities += entity
    _modCount += 1

  def removeEntity(entity: Entity): Unit =
    chunkData.entities -= entity
    _modCount += 1

  def blocks: Array[LocalBlockState] = chunkData.storage.allBlocks

  def getBlock(coords: BlockRelChunk): BlockState = chunkData.storage.getBlock(coords)

  def setBlock(blockCoords: BlockRelChunk, block: BlockState): Unit =
    val before = getBlock(blockCoords)
    if before != block then
      chunkData.storage.setBlock(blockCoords, block)
      _modCount += 1

  def optimizeStorage(): Unit = chunkData.optimizeStorage()

  def toNbt: Nbt.MapTag = chunkData.toNBT

  def isDecorated: Boolean = chunkData.isDecorated
  def setDecorated(): Unit =
    if !chunkData.isDecorated then
      chunkData.isDecorated = true
      _modCount += 1

class ChunkData(
    private[chunk] var storage: ChunkStorage,
    private[chunk] val entities: mutable.ArrayBuffer[Entity],
    private[chunk] var isDecorated: Boolean
):
  def optimizeStorage(): Unit =
    if storage.isDense then {
      if storage.numBlocks < 32
      then storage = SparseChunkStorage.fromStorage(storage)
    } else {
      if storage.numBlocks > 48
      then storage = DenseChunkStorage.fromStorage(storage)
    }

  def toNBT: Nbt.MapTag =
    val storageNbt = storage.toNBT

    Nbt.makeMap(
      "blocks" -> Nbt.ByteArrayTag(storageNbt.blocks),
      "metadata" -> Nbt.ByteArrayTag(storageNbt.metadata),
      "entities" -> Nbt.ListTag(entities.map(e => e.toNBT).toSeq),
      "isDecorated" -> Nbt.ByteTag(isDecorated)
    )

object ChunkData:
  def fromStorage(storage: ChunkStorage): ChunkData =
    new ChunkData(storage, mutable.ArrayBuffer.empty, false)

  def fromNBT(nbt: Nbt.MapTag)(using CylinderSize): ChunkData =
    val storage = nbt.getByteArray("blocks") match
      case Some(blocks) =>
        val meta = nbt.getByteArray("metadata")
        DenseChunkStorage.fromNBT(blocks.toArray, meta.map(_.toArray))
      case None =>
        SparseChunkStorage.empty

    val entities = mutable.ArrayBuffer.empty[Entity]
    for
      tags <- nbt.getList("entities")
      tag <- tags.map(_.asInstanceOf[Nbt.MapTag])
    do
      EntityFactory.fromNbt(tag) match
        case Ok(entity)   => entities += entity
        case Err(message) => println(message)

    val isDecorated = nbt.getBoolean("isDecorated", default = false)

    new ChunkData(storage, entities, isDecorated)

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
