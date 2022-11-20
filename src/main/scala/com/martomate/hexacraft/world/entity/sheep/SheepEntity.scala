package com.martomate.hexacraft.world.entity.sheep

import com.flowpowered.nbt.{CompoundTag, StringTag, Tag}
import com.martomate.hexacraft.util.{CylinderSize, NBTUtil}
import com.martomate.hexacraft.world.{BlocksInWorld, CollisionDetector}
import com.martomate.hexacraft.world.block.{Blocks, HexBox}
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.entity.{EntityFactory, EntityModel, EntityModelLoader}
import com.martomate.hexacraft.world.entity.ai.{EntityAI, EntityAIFactory}
import com.martomate.hexacraft.world.entity.base.BasicEntity

class SheepEntity(
    _model: EntityModel,
    private val ai: EntityAI[SheepEntity]
)(using CylinderSize, Blocks)
    extends BasicEntity(_model) {
  override val boundingBox: HexBox = new HexBox(0.4f, 0, 0.75f)

  override def id: String = "sheep"

  override def tick(world: BlocksInWorld, collisionDetector: CollisionDetector): Unit = {
    ai.tick(world, this)
    velocity.add(ai.acceleration())

    velocity.x *= 0.9
    velocity.z *= 0.9
    super.tick(world, collisionDetector)
  }

  override def toNBT: Seq[Tag[_]] =
    super.toNBT :+ new StringTag("type", "sheep") :+ NBTUtil.makeCompoundTag("ai", ai.toNBT)
}

object SheepEntity extends EntityFactory[SheepEntity] {
  def atStartPos(
      pos: CylCoords,
      aiFactory: EntityAIFactory[SheepEntity],
      model: EntityModel
  )(using CylinderSize, Blocks): SheepEntity = {
    val pl = new SheepEntity(model, aiFactory.makeEntityAI)
    pl.position = pos
    pl
  }

  override def createEntity(using modelFactory: EntityModelLoader, cylSize: CylinderSize, BLocks: Blocks): SheepEntity =
    new SheepEntity(modelFactory.load("sheep"), SheepAIFactory.makeEntityAI)

  override def fromNBT(tag: CompoundTag)(using EntityModelLoader, CylinderSize, Blocks): SheepEntity =
    val entity = super.fromNBT(tag)
    NBTUtil.getCompoundTag(tag, "ai").foreach(aiTag => entity.ai.fromNBT(aiTag))
    entity
}
