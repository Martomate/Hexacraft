package com.martomate.hexacraft.world.entity.ai

import com.martomate.hexacraft.world.entity.Entity
import com.martomate.hexacraft.world.worldlike.IWorld

trait EntityAIFactory[E <: Entity] {
  def makeEntityAI(world: IWorld, entity: E): EntityAI
}
