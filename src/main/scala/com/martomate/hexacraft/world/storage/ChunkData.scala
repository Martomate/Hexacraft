package com.martomate.hexacraft.world.storage

import com.flowpowered.nbt.{ByteTag, CompoundTag, Tag}
import com.martomate.hexacraft.util.{CylinderSize, NBTUtil}
import com.martomate.hexacraft.world.EntitiesInChunkImpl
import com.martomate.hexacraft.world.chunk.EntitiesInChunk
import com.martomate.hexacraft.world.worldlike.IWorld

class ChunkData(init_storage: ChunkStorage, world: IWorld)(implicit cylSize: CylinderSize) {
  var storage: ChunkStorage = init_storage
  val entities: EntitiesInChunk = new EntitiesInChunkImpl(world)
  var isDecorated: Boolean = false

  def optimizeStorage(): Unit = {
    if (storage.isDense) {
      if (storage.numBlocks < 32) {
        storage = new SparseChunkStorage(storage)
      }
    } else {
      if (storage.numBlocks > 48) {
        storage = new DenseChunkStorage(storage)
      }
    }
  }

  def fromNBT(nbt: CompoundTag): Unit = {
    storage.fromNBT(nbt)
    entities.fromNBT(nbt)
    isDecorated = NBTUtil.getBoolean(nbt, "isDecorated", default = false)
  }

  def toNBT: Seq[Tag[_]] = {
    storage.toNBT ++ entities.toNBT :+ new ByteTag("isDecorated", isDecorated)
  }
}
