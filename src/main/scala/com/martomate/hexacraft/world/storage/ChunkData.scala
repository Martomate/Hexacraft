package com.martomate.hexacraft.world.storage

class ChunkData {
  var storage: ChunkStorage = _

  def optimizeStorage(): Unit = {
    if (storage.isDense) {
      if (storage.numBlocks < 48) {
        storage = storage.toSparse
      }
    } else {
      if (storage.numBlocks > 64) {
        storage = storage.toDense
      }
    }
  }
}
