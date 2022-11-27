package com.martomate.hexacraft.world.entity.ai

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.BlocksInWorld
import com.martomate.hexacraft.world.block.Blocks
import com.martomate.hexacraft.world.entity.Entity

import com.flowpowered.nbt.CompoundTag

trait EntityAIFactory[E <: Entity] {
  def makeEntityAI(using CylinderSize, Blocks): EntityAI[E]

  def fromNBT(tag: CompoundTag)(using CylinderSize, Blocks): EntityAI[E]
}
