package com.martomate.hexacraft.world.loader

import com.martomate.hexacraft.world.World
import com.martomate.hexacraft.world.chunk.Chunk
import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld

trait ChunkLoader {
  def tick(): Unit
  def chunksToAdd(): Iterable[Chunk]
  def chunksToRemove(): Iterable[ChunkRelWorld]
  def onWorldEvent(event: World.Event): Unit
  def unload(): Unit
}
