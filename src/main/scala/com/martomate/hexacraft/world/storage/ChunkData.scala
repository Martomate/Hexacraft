package com.martomate.hexacraft.world.storage

import com.flowpowered.nbt.{CompoundTag, Tag}
import com.martomate.hexacraft.world.EntitiesInChunkImpl
import com.martomate.hexacraft.world.chunk.EntitiesInChunk

class ChunkData {
  var storage: ChunkStorage = _
  val entities: EntitiesInChunk = new EntitiesInChunkImpl

  def optimizeStorage(): Unit = {
    if (storage.isDense) {
      if (storage.numBlocks < 48) {
        storage = new SparseChunkStorage(storage)
      }
    } else {
      if (storage.numBlocks > 64) {
        storage = new DenseChunkStorage(storage)
      }
    }
  }

  def fromNBT(nbt: CompoundTag): Unit = {
    storage.fromNBT(nbt)
    entities.fromNBT(nbt)
  }

  def toNBT: Seq[Tag[_]] = {
    storage.toNBT ++ entities.toNBT
  }
}
