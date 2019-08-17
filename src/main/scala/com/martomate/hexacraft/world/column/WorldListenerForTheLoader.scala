package com.martomate.hexacraft.world.column

import com.martomate.hexacraft.world.chunk.ChunkAddedOrRemovedListener

trait WorldListenerForTheLoader extends ChunkAddedOrRemovedListener {
  def onColumnAdded(column: ChunkColumn): Unit
  def onColumnRemoved(column: ChunkColumn): Unit
}
