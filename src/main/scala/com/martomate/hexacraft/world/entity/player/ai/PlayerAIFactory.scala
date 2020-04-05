package com.martomate.hexacraft.world.entity.player.ai

import com.martomate.hexacraft.world.entity.ai.EntityAIFactory
import com.martomate.hexacraft.world.entity.player.PlayerEntity
import com.martomate.hexacraft.world.worldlike.IWorld

object PlayerAIFactory extends EntityAIFactory[PlayerEntity] {
  override def makeEntityAI(world: IWorld, player: PlayerEntity): SimplePlayerAI = {
    new SimplePlayerAI(player, new SimplePlayerAIInput(world, player))
  }
}
