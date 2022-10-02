package com.martomate.hexacraft.world.entity.sheep

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.BlocksInWorld
import com.martomate.hexacraft.world.entity.ai.{EntityAI, EntityAIFactory, SimpleAIInput, SimpleWalkAI}

object SheepAIFactory extends EntityAIFactory[SheepEntity] {
  override def makeEntityAI(world: BlocksInWorld, sheep: SheepEntity)(implicit
      cylSize: CylinderSize
  ): EntityAI = {
    new SimpleWalkAI(sheep, new SimpleAIInput(world))
  }
}
