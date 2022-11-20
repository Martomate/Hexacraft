package com.martomate.hexacraft.world.chunk

import com.flowpowered.nbt.{ByteTag, CompoundTag, Tag}
import com.martomate.hexacraft.util.{CylinderSize, NBTUtil}
import com.martomate.hexacraft.world.BlocksInWorld
import com.martomate.hexacraft.world.block.Blocks
import com.martomate.hexacraft.world.chunk.storage.{ChunkStorage, DenseChunkStorage, SparseChunkStorage}
import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld
import com.martomate.hexacraft.world.entity.EntityRegistry

class ChunkData(var storage: ChunkStorage, val entities: EntitiesInChunk)(using CylinderSize, Blocks):
  var isDecorated: Boolean = false

  def optimizeStorage(): Unit =
    if storage.isDense then {
      if storage.numBlocks < 32
      then storage = SparseChunkStorage.fromStorage(storage)
    } else {
      if storage.numBlocks > 48
      then storage = DenseChunkStorage.fromStorage(storage)
    }

  def toNBT: Seq[Tag[_]] =
    storage.toNBT ++ entities.toNBT :+ new ByteTag("isDecorated", isDecorated)

object ChunkData:
  def fromStorage(storage: ChunkStorage)(using CylinderSize, Blocks): ChunkData =
    new ChunkData(storage, EntitiesInChunk.empty)

  def fromNBT(nbt: CompoundTag)(coords: ChunkRelWorld, world: BlocksInWorld, registry: EntityRegistry)(using
      CylinderSize,
      Blocks
  ): ChunkData =
    val storage = DenseChunkStorage.fromNBT(nbt)(coords)
    val entitiesInChunk = EntitiesInChunk.fromNBT(nbt)(world, registry)

    val data = new ChunkData(storage, entitiesInChunk)
    data.isDecorated = NBTUtil.getBoolean(nbt, "isDecorated", default = false)
    data
