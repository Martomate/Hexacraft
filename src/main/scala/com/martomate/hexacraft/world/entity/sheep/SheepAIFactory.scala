package com.martomate.hexacraft.world.entity.sheep

import com.flowpowered.nbt.CompoundTag
import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.BlocksInWorld
import com.martomate.hexacraft.world.block.Blocks
import com.martomate.hexacraft.world.entity.ai.{EntityAI, EntityAIFactory, SimpleAIInput, SimpleWalkAI}

object SheepAIFactory extends EntityAIFactory[SheepEntity] {
  override def makeEntityAI(using CylinderSize, Blocks): EntityAI[SheepEntity] =
    new SimpleWalkAI(new SimpleAIInput)

  override def fromNBT(tag: CompoundTag)(using CylinderSize, Blocks): EntityAI[SheepEntity] =
    val ai = makeEntityAI
    ai.fromNBT(tag)
    ai
}
