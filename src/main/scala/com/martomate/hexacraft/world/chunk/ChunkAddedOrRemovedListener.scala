package com.martomate.hexacraft.world.chunk

trait ChunkAddedOrRemovedListener {
  def onChunkAdded(chunk: IChunk): Unit
  def onChunkRemoved(chunk: IChunk): Unit
}
