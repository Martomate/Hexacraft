package com.martomate.hexacraft.world.entity.player.ai

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.entity.ai.EntityAIFactory
import com.martomate.hexacraft.world.entity.player.PlayerEntity
import com.martomate.hexacraft.world.worldlike.BlocksInWorld

object PlayerAIFactory extends EntityAIFactory[PlayerEntity] {
  override def makeEntityAI(world: BlocksInWorld, player: PlayerEntity)(implicit cylSize: CylinderSize): SimplePlayerAI = {
    new SimplePlayerAI(player, new SimplePlayerAIInput(world, player))
  }
}
