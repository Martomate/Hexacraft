package com.martomate.hexacraft.world.entity.player

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.BlocksInWorld
import com.martomate.hexacraft.world.block.Blocks
import com.martomate.hexacraft.world.entity.ai.{EntityAI, EntityAIFactory, SimpleAIInput, SimpleWalkAI}

object PlayerAIFactory extends EntityAIFactory[PlayerEntity] {
  override def makeEntityAI(world: BlocksInWorld, player: PlayerEntity)(using
      CylinderSize,
      Blocks
  ): EntityAI = {
    new SimpleWalkAI(player, new SimpleAIInput(world))
  }
}
