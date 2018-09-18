package com.martomate.hexacraft.world.temp2

import com.martomate.hexacraft.world.chunk.IChunk

trait ChunkAddedOrRemovedListener {
  def onChunkAdded(chunk: IChunk): Unit
  def onChunkRemoved(chunk: IChunk): Unit
}
