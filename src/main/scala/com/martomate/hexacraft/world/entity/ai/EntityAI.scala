package com.martomate.hexacraft.world.entity.ai

import com.flowpowered.nbt.{CompoundTag, Tag}
import com.martomate.hexacraft.world.BlocksInWorld
import com.martomate.hexacraft.world.entity.Entity
import org.joml.Vector3dc

abstract class EntityAI[E <: Entity] {
  def tick(world: BlocksInWorld, entity: E): Unit
  def acceleration(): Vector3dc

  def fromNBT(tag: CompoundTag): Unit
  def toNBT: Seq[Tag[_]]
}
