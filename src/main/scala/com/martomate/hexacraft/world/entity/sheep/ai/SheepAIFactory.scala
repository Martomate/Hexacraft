package com.martomate.hexacraft.world.entity.sheep.ai

import com.martomate.hexacraft.world.entity.ai.EntityAIFactory
import com.martomate.hexacraft.world.entity.sheep.SheepEntity
import com.martomate.hexacraft.world.worldlike.IWorld

object SheepAIFactory extends EntityAIFactory[SheepEntity] {
  override def makeEntityAI(world: IWorld, sheep: SheepEntity): SimpleSheepAI = {
    new SimpleSheepAI(sheep, new SimpleSheepAIInput(world, sheep))
  }
}
