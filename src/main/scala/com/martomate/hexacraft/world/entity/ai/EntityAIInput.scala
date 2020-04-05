package com.martomate.hexacraft.world.entity.ai

import com.martomate.hexacraft.world.block.Block

trait EntityAIInput {
  def blockInFront(dist: Double): Block
}
