package com.martomate.hexacraft.world.chunk

import com.martomate.hexacraft.world.coord.integer.{BlockRelWorld, ChunkRelWorld}

trait ChunkEventListener {
  def onBlockNeedsUpdate(coords: BlockRelWorld): Unit
  def onChunkNeedsRenderUpdate(coords: ChunkRelWorld): Unit

  @deprecated
  def onChunksNeighborNeedsRenderUpdate(coords: ChunkRelWorld, side: Int): Unit
}
