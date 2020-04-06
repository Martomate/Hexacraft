package com.martomate.hexacraft.world.loader

import com.martomate.hexacraft.world.chunk.{ChunkAddedOrRemovedListener, IChunk}
import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld

trait ChunkLoader extends ChunkAddedOrRemovedListener {
  def tick(): Unit
  def chunksToAdd(): Iterable[IChunk]
  def chunksToRemove(): Iterable[ChunkRelWorld]
  def unload(): Unit
}
