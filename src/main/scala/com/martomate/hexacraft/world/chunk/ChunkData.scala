package com.martomate.hexacraft.world.chunk

import com.martomate.hexacraft.nbt.{Nbt, NBTUtil}
import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.util.Result.{Err, Ok}
import com.martomate.hexacraft.world.BlocksInWorld
import com.martomate.hexacraft.world.block.{Blocks, BlockState}
import com.martomate.hexacraft.world.chunk.storage.{
  ChunkStorage,
  DenseChunkStorage,
  LocalBlockState,
  SparseChunkStorage
}
import com.martomate.hexacraft.world.coord.integer.{BlockRelChunk, ChunkRelWorld}
import com.martomate.hexacraft.world.entity.{Entity, EntityRegistry}

import com.flowpowered.nbt.{ByteArrayTag, ByteTag, CompoundTag, ListTag, Tag}
import scala.collection.immutable.ArraySeq
import scala.collection.mutable

class ChunkData(private var storage: ChunkStorage, entities: mutable.ArrayBuffer[Entity])(using CylinderSize, Blocks):
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
  def allEntities: Seq[Entity] = entities.toSeq

  def setDecorated(): Unit = _isDecorated = true

  def toNBT: Nbt.MapTag =
    val storageNbt = storage.toNBT

    Nbt.makeMap(
      "blocks" -> Nbt.ByteArrayTag(storageNbt.blocks),
      "metadata" -> Nbt.ByteArrayTag(storageNbt.metadata),
      "entities" -> Nbt.ListTag(allEntities.map(e => e.toNBT)),
      "isDecorated" -> Nbt.ByteTag(_isDecorated)
    )

object ChunkData:
  def fromStorage(storage: ChunkStorage)(using CylinderSize, Blocks): ChunkData =
    new ChunkData(storage, mutable.ArrayBuffer.empty)

  def fromNBT(nbt: Nbt.MapTag)(registry: EntityRegistry)(using CylinderSize, Blocks): ChunkData =
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
      CylinderSize,
      Blocks
  ): Iterable[Entity] =
    val entities = mutable.ArrayBuffer.empty[Entity]

    for tag <- list do
      Entity.fromNbt(tag, registry) match
        case Ok(entity)   => entities += entity
        case Err(message) => println(message)

    entities
