package com.martomate.hexacraft.world.chunk

trait ChunkAddedOrRemovedListener {
  def onChunkAdded(chunk: Chunk): Unit
  def onChunkRemoved(chunk: Chunk): Unit
}
