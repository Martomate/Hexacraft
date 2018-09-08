package com.martomate.hexacraft.world.storage

import com.martomate.hexacraft.world.coord.ChunkRelWorld

object ChunkLoader {
  val chunksLoadedPerTick = 2
  val ticksBetweenColumnLoading = 5
}

trait ChunkLoader extends WorldListenerForTheLoader {
  def tick(): Unit
  def chunksToAdd(): Iterable[IChunk]
  def chunksToRemove(): Iterable[ChunkRelWorld]
  def unload(): Unit
}
