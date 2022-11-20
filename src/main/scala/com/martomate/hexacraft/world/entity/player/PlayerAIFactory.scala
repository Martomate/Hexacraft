package com.martomate.hexacraft.world.entity.player

import com.flowpowered.nbt.CompoundTag
import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.BlocksInWorld
import com.martomate.hexacraft.world.block.Blocks
import com.martomate.hexacraft.world.entity.ai.{EntityAI, EntityAIFactory, SimpleAIInput, SimpleWalkAI}

object PlayerAIFactory extends EntityAIFactory[PlayerEntity] {
  override def makeEntityAI(using CylinderSize, Blocks): EntityAI[PlayerEntity] =
    new SimpleWalkAI(new SimpleAIInput)

  override def fromNBT(tag: CompoundTag)(using CylinderSize, Blocks): EntityAI[PlayerEntity] =
    val ai = makeEntityAI
    ai.fromNBT(tag)
    ai
}
