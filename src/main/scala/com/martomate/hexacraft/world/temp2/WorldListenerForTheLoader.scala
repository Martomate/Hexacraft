package com.martomate.hexacraft.world.temp2

trait WorldListenerForTheLoader extends ChunkAddedOrRemovedListener {
  def onColumnAdded(column: ChunkColumn): Unit
  def onColumnRemoved(column: ChunkColumn): Unit
}
