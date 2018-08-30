package com.martomate.hexacraft.world.storage

trait WorldListenerForTheLoader extends ChunkAddedOrRemovedListener {
  def onColumnAdded(column: ChunkColumn): Unit
  def onColumnRemoved(column: ChunkColumn): Unit
}
