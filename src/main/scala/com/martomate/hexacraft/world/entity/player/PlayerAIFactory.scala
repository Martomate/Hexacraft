package com.martomate.hexacraft.world.entity.player

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.BlocksInWorld
import com.martomate.hexacraft.world.entity.ai.{
  EntityAI,
  EntityAIFactory,
  SimpleAIInput,
  SimpleWalkAI
}

object PlayerAIFactory extends EntityAIFactory[PlayerEntity] {
  override def makeEntityAI(world: BlocksInWorld, player: PlayerEntity)(implicit
      cylSize: CylinderSize
  ): EntityAI = {
    new SimpleWalkAI(player, new SimpleAIInput(world))
  }
}
