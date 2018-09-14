package com.martomate.hexacraft.world.chunk

import com.martomate.hexacraft.world.storage.{ChunkStorage, DenseChunkStorage, SparseChunkStorage}

class ChunkData {
  var storage: ChunkStorage = _

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
}
