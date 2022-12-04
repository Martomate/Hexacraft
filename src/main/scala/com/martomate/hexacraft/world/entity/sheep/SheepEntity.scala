package com.martomate.hexacraft.world.entity.sheep

import com.martomate.hexacraft.util.{CylinderSize, NBTUtil}
import com.martomate.hexacraft.world.{BlocksInWorld, CollisionDetector}
import com.martomate.hexacraft.world.block.{Blocks, HexBox}
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.entity.*
import com.martomate.hexacraft.world.entity.ai.{EntityAI, SimpleAIInput, SimpleWalkAI}

import com.flowpowered.nbt.{CompoundTag, StringTag, Tag}

class SheepEntity(
    val model: EntityModel,
    initData: EntityBaseData,
    private val ai: EntityAI
)(using CylinderSize, Blocks)
    extends Entity(initData) {
  override val boundingBox: HexBox = new HexBox(0.4f, 0, 0.75f)

  override def id: String = "sheep"

  override def tick(world: BlocksInWorld, collisionDetector: CollisionDetector): Unit = {
    ai.tick(world, data, boundingBox)
    data.velocity.add(ai.acceleration())

    data.velocity.x *= 0.9
    data.velocity.z *= 0.9

    EntityPhysicsSystem(world, collisionDetector).update(data, boundingBox)
    model.tick()
  }

  override def toNBT: Seq[Tag[_]] =
    super.toNBT :+ new StringTag("type", "sheep") :+ NBTUtil.makeCompoundTag("ai", ai.toNBT)
}
