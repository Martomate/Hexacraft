package com.martomate.hexacraft.world.entity.player

import com.flowpowered.nbt.{CompoundTag, StringTag, Tag}
import com.martomate.hexacraft.util.{CylinderSize, NBTUtil}
import com.martomate.hexacraft.world.{BlocksInWorld, CollisionDetector}
import com.martomate.hexacraft.world.block.{Blocks, HexBox}
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.entity.{Entity, EntityBaseData, EntityFactory, EntityModel, EntityModelLoader}
import com.martomate.hexacraft.world.entity.ai.{EntityAI, EntityAIFactory, SimpleWalkAI}
import com.martomate.hexacraft.world.entity.base.BasicEntity

class PlayerEntity(
    _model: EntityModel,
    initData: EntityBaseData,
    private val ai: EntityAI[PlayerEntity]
)(using CylinderSize, Blocks)
    extends BasicEntity(_model, initData) {
  override val boundingBox: HexBox = new HexBox(0.2f, 0, 1.75f)

  override def id: String = "player"

  override def tick(world: BlocksInWorld, collisionDetector: CollisionDetector): Unit = {
    ai.tick(world, data, boundingBox)
    velocity.add(ai.acceleration())

    velocity.x *= 0.9
    velocity.z *= 0.9

    super.tick(world, collisionDetector)
  }

  override def toNBT: Seq[Tag[_]] =
    super.toNBT :+ new StringTag("type", "player") :+ NBTUtil.makeCompoundTag("ai", ai.toNBT)
}

object PlayerEntity extends EntityFactory[PlayerEntity]:
  override def atStartPos(pos: CylCoords)(using EntityModelLoader, CylinderSize, Blocks): PlayerEntity =
    val model = summon[EntityModelLoader].load("player")
    new PlayerEntity(model, new EntityBaseData(position = pos), SimpleWalkAI.create)

  override def fromNBT(tag: CompoundTag)(using EntityModelLoader, CylinderSize, Blocks): PlayerEntity =
    val model = summon[EntityModelLoader].load("player")
    val baseData = EntityBaseData.fromNBT(tag)
    val ai: EntityAI[PlayerEntity] =
      NBTUtil.getCompoundTag(tag, "ai") match
        case Some(t) => SimpleWalkAI.fromNBT(t)
        case None    => SimpleWalkAI.create

    new PlayerEntity(model, baseData, ai)
