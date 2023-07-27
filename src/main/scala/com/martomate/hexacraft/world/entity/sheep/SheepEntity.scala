package com.martomate.hexacraft.world.entity.sheep

import com.flowpowered.nbt.{CompoundTag, StringTag, Tag}
import com.martomate.hexacraft.nbt.{Nbt, NBTUtil}
import com.martomate.hexacraft.world.{BlocksInWorld, CollisionDetector, CylinderSize}
import com.martomate.hexacraft.world.block.{Blocks, HexBox}
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.entity.*
import com.martomate.hexacraft.world.entity.ai.{EntityAI, SimpleAIInput, SimpleWalkAI}

class SheepEntity(
    model: EntityModel,
    initData: EntityBaseData,
    private val ai: EntityAI
)(using CylinderSize)
    extends Entity(initData, model) {
  override val boundingBox: HexBox = new HexBox(0.4f, 0, 0.75f)

  override def tick(world: BlocksInWorld, collisionDetector: CollisionDetector): Unit = {
    ai.tick(world, data, boundingBox)
    data.velocity.add(ai.acceleration())

    data.velocity.x *= 0.9
    data.velocity.z *= 0.9

    EntityPhysicsSystem(world, collisionDetector).update(data, boundingBox)
    model.tick()
  }

  override def toNBT: Nbt.MapTag =
    super.toNBT
      .withField("type", Nbt.StringTag("sheep"))
      .withField("ai", Nbt.from(NBTUtil.makeCompoundTag("ai", ai.toNBT)))
}
