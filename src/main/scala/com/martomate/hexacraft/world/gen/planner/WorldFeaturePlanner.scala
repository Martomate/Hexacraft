package com.martomate.hexacraft.world.gen.planner

import com.martomate.hexacraft.world.chunk.IChunk
import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld

trait WorldFeaturePlanner {
  def decorate(chunk: IChunk): Unit
  def plan(coords: ChunkRelWorld): Unit
}
