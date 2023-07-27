package com.martomate.hexacraft.world.entity.ai

import com.flowpowered.nbt.{CompoundTag, Tag}
import com.martomate.hexacraft.world.BlocksInWorld
import com.martomate.hexacraft.world.block.HexBox
import com.martomate.hexacraft.world.entity.{Entity, EntityBaseData}
import org.joml.Vector3dc

trait EntityAI {
  def tick(world: BlocksInWorld, entityBaseData: EntityBaseData, entityBoundingBox: HexBox): Unit
  def acceleration(): Vector3dc

  def toNBT: Seq[Tag[_]]
}
