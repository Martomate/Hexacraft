package com.martomate.hexacraft.world.storage

import com.martomate.hexacraft.world.coord.{BlockRelWorld, ChunkRelWorld}

trait ChunkEventListener {
  def onBlockNeedsUpdate(coords: BlockRelWorld): Unit
  def onChunkNeedsRenderUpdate(coords: ChunkRelWorld): Unit
  def onChunksNeighborNeedsRenderUpdate(coords: ChunkRelWorld, side: Int): Unit
}
