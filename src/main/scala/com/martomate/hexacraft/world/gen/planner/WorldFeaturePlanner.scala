package com.martomate.hexacraft.world.gen.planner

import com.martomate.hexacraft.world.chunk.IChunk
import com.martomate.hexacraft.world.worldlike.IWorld

trait WorldFeaturePlanner {
  def decorate(chunk: IChunk, world: IWorld): Unit
}
