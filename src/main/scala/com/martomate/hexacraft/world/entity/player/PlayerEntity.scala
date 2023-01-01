package com.martomate.hexacraft.world.entity.player

import com.martomate.hexacraft.util.{CylinderSize, NBTUtil}
import com.martomate.hexacraft.world.{BlocksInWorld, CollisionDetector}
import com.martomate.hexacraft.world.block.{Blocks, HexBox}
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.entity.*
import com.martomate.hexacraft.world.entity.ai.{EntityAI, SimpleWalkAI}

import com.flowpowered.nbt.{CompoundTag, StringTag, Tag}

class PlayerEntity(
    model: EntityModel,
    initData: EntityBaseData,
    private val ai: EntityAI
)(using CylinderSize)
    extends Entity(initData, model) {
  override val boundingBox: HexBox = new HexBox(0.2f, 0, 1.75f)

  override def tick(world: BlocksInWorld, collisionDetector: CollisionDetector): Unit = {
    ai.tick(world, data, boundingBox)
    data.velocity.add(ai.acceleration())

    data.velocity.x *= 0.9
    data.velocity.z *= 0.9

    EntityPhysicsSystem(world, collisionDetector).update(data, boundingBox)
    model.tick()
  }

  override def toNBT: Seq[Tag[_]] =
    super.toNBT :+ new StringTag("type", "player") :+ NBTUtil.makeCompoundTag("ai", ai.toNBT)
}
