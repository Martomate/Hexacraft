package com.martomate.hexacraft.world.storage

import com.martomate.hexacraft.world.chunk.IChunk

trait ChunkAddedOrRemovedListener {
  def onChunkAdded(chunk: IChunk): Unit
  def onChunkRemoved(chunk: IChunk): Unit
}
