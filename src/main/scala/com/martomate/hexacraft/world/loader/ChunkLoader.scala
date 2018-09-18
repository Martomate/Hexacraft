package com.martomate.hexacraft.world.loader

import com.martomate.hexacraft.world.chunk.IChunk
import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld
import com.martomate.hexacraft.world.temp2.WorldListenerForTheLoader

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
