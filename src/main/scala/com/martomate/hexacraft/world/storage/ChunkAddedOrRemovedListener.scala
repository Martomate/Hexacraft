package com.martomate.hexacraft.world.storage

trait ChunkAddedOrRemovedListener {
  def onChunkAdded(chunk: Chunk): Unit
  def onChunkRemoved(chunk: Chunk): Unit
}
