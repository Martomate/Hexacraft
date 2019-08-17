package com.martomate.hexacraft.world.loader

import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld

trait ChunkLoadingEdgeListener {
  def onChunkOnEdge(chunk: ChunkRelWorld, onEdge: Boolean): Unit
  def onChunkLoadable(chunk: ChunkRelWorld, loadable: Boolean): Unit
}
