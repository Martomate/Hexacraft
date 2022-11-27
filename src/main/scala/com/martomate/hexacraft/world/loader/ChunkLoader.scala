package com.martomate.hexacraft.world.loader

import com.martomate.hexacraft.world.chunk.{Chunk, ChunkAddedOrRemovedListener}
import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld

trait ChunkLoader extends ChunkAddedOrRemovedListener {
  def tick(): Unit
  def chunksToAdd(): Iterable[Chunk]
  def chunksToRemove(): Iterable[ChunkRelWorld]
  def unload(): Unit
}
