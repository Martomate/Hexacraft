package com.martomate.hexacraft.world.entity.sheep

import com.martomate.hexacraft.util.{CylinderSize, NBTUtil}
import com.martomate.hexacraft.world.{BlocksInWorld, CollisionDetector}
import com.martomate.hexacraft.world.block.{Blocks, HexBox}
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.entity.{EntityBaseData, EntityFactory, EntityModel, EntityModelLoader}
import com.martomate.hexacraft.world.entity.ai.{EntityAI, EntityAIFactory, SimpleAIInput, SimpleWalkAI}
import com.martomate.hexacraft.world.entity.base.BasicEntity

import com.flowpowered.nbt.{CompoundTag, StringTag, Tag}

class SheepEntity(
    _model: EntityModel,
    initData: EntityBaseData,
    private val ai: EntityAI[SheepEntity]
)(using CylinderSize, Blocks)
    extends BasicEntity(_model, initData) {
  override val boundingBox: HexBox = new HexBox(0.4f, 0, 0.75f)

  override def id: String = "sheep"

  override def tick(world: BlocksInWorld, collisionDetector: CollisionDetector): Unit = {
    ai.tick(world, data, boundingBox)
    velocity.add(ai.acceleration())

    velocity.x *= 0.9
    velocity.z *= 0.9
    super.tick(world, collisionDetector)
  }

  override def toNBT: Seq[Tag[_]] =
    super.toNBT :+ new StringTag("type", "sheep") :+ NBTUtil.makeCompoundTag("ai", ai.toNBT)
}

object SheepEntity extends EntityFactory[SheepEntity]:
  override def atStartPos(pos: CylCoords)(using EntityModelLoader, CylinderSize, Blocks): SheepEntity =
    val model = summon[EntityModelLoader].load("sheep")
    new SheepEntity(model, new EntityBaseData(position = pos), SimpleWalkAI.create)

  override def fromNBT(tag: CompoundTag)(using EntityModelLoader, CylinderSize, Blocks): SheepEntity =
    val model = summon[EntityModelLoader].load("sheep")
    val baseData = EntityBaseData.fromNBT(tag)
    val ai: EntityAI[SheepEntity] =
      NBTUtil.getCompoundTag(tag, "ai") match
        case Some(t) => SimpleWalkAI.fromNBT(t)
        case None    => SimpleWalkAI.create
    new SheepEntity(model, baseData, ai)
