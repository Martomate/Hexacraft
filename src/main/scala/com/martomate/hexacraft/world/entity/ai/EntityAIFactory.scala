package com.martomate.hexacraft.world.entity.ai

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.BlocksInWorld
import com.martomate.hexacraft.world.entity.Entity

trait EntityAIFactory[E <: Entity] {
  def makeEntityAI(world: BlocksInWorld, entity: E)(implicit cylSize: CylinderSize): EntityAI
}
