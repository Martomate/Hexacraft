package com.martomate.hexacraft.world.storage

trait ChunkAddedOrRemovedListener {
  def onChunkAdded(chunk: IChunk): Unit
  def onChunkRemoved(chunk: IChunk): Unit
}
