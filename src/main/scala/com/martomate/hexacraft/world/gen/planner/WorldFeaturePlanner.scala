package com.martomate.hexacraft.world.gen.planner

import com.martomate.hexacraft.world.chunk.Chunk
import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld

trait WorldFeaturePlanner {
  def decorate(chunk: Chunk): Unit
  def plan(coords: ChunkRelWorld): Unit
}
