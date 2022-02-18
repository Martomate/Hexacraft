package com.martomate.hexacraft.world.entity.sheep.ai

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.BlocksInWorld
import com.martomate.hexacraft.world.entity.ai.EntityAIFactory
import com.martomate.hexacraft.world.entity.sheep.SheepEntity

object SheepAIFactory extends EntityAIFactory[SheepEntity] {
  override def makeEntityAI(world: BlocksInWorld, sheep: SheepEntity)(implicit cylSize: CylinderSize): SimpleSheepAI = {
    new SimpleSheepAI(sheep, new SimpleSheepAIInput(world, sheep))
  }
}
