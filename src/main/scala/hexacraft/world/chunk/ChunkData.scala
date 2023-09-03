package hexacraft.world.chunk

import hexacraft.nbt.Nbt
import hexacraft.util.Result.{Err, Ok}
import hexacraft.world.CylinderSize
import hexacraft.world.block.BlockState
import hexacraft.world.chunk.storage.{ChunkStorage, DenseChunkStorage, LocalBlockState, SparseChunkStorage}
import hexacraft.world.coord.integer.BlockRelChunk
import hexacraft.world.entity.{Entity, EntityRegistry}

import scala.collection.{mutable, SeqView}
import scala.collection.immutable.ArraySeq

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
