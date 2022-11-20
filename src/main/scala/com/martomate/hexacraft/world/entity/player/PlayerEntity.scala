package com.martomate.hexacraft.world.entity.player

import com.flowpowered.nbt.{CompoundTag, StringTag, Tag}
import com.martomate.hexacraft.util.{CylinderSize, NBTUtil}
import com.martomate.hexacraft.world.{BlocksInWorld, CollisionDetector}
import com.martomate.hexacraft.world.block.{Blocks, HexBox}
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.entity.{Entity, EntityFactory, EntityModel, EntityModelLoader}
import com.martomate.hexacraft.world.entity.ai.{EntityAI, EntityAIFactory}
import com.martomate.hexacraft.world.entity.base.BasicEntity

class PlayerEntity(
    _model: EntityModel,
    private val ai: EntityAI[PlayerEntity]
)(using CylinderSize, Blocks)
    extends BasicEntity(_model) {
  override val boundingBox: HexBox = new HexBox(0.2f, 0, 1.75f)

  override def id: String = "player"

  override def tick(world: BlocksInWorld, collisionDetector: CollisionDetector): Unit = {
    ai.tick(world, this)
    velocity.add(ai.acceleration())

    velocity.x *= 0.9
    velocity.z *= 0.9

    super.tick(world, collisionDetector)
  }

  override def toNBT: Seq[Tag[_]] =
    super.toNBT :+ new StringTag("type", "player") :+ NBTUtil.makeCompoundTag("ai", ai.toNBT)
}

object PlayerEntity extends EntityFactory[PlayerEntity] {
  def atStartPos(
      pos: CylCoords,
      aiFactory: EntityAIFactory[PlayerEntity],
      model: EntityModel
  )(using CylinderSize, Blocks): PlayerEntity = {
    val pl = new PlayerEntity(model, aiFactory.makeEntityAI)
    pl.position = pos
    pl
  }

  override def createEntity(using
      modelFactory: EntityModelLoader,
      cylSize: CylinderSize,
      Blocks: Blocks
  ): PlayerEntity =
    new PlayerEntity(modelFactory.load("player"), PlayerAIFactory.makeEntityAI)

  override def fromNBT(tag: CompoundTag)(using EntityModelLoader, CylinderSize, Blocks): PlayerEntity =
    val entity = super.fromNBT(tag)
    NBTUtil.getCompoundTag(tag, "ai").foreach(aiTag => entity.ai.fromNBT(aiTag))
    entity
}
