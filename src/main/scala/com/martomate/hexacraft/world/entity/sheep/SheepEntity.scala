package com.martomate.hexacraft.world.entity.sheep

import com.flowpowered.nbt.{CompoundTag, StringTag, Tag}
import com.martomate.hexacraft.util.{CylinderSize, NBTUtil}
import com.martomate.hexacraft.world.{BlocksInWorld, CollisionDetector}
import com.martomate.hexacraft.world.block.HexBox
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.entity.EntityModel
import com.martomate.hexacraft.world.entity.ai.{EntityAI, EntityAIFactory}
import com.martomate.hexacraft.world.entity.base.BasicEntity

class SheepEntity(_model: EntityModel, world: BlocksInWorld, aiFactory: EntityAIFactory[SheepEntity])(implicit cylSizeImpl: CylinderSize) extends BasicEntity(_model) {
  override val boundingBox: HexBox = new HexBox(0.4f, 0, 0.75f)

  override def id: String = "sheep"

  private val ai: EntityAI = aiFactory.makeEntityAI(world, this)

  override def tick(collisionDetector: CollisionDetector): Unit = {
    ai.tick()
    velocity.add(ai.acceleration())

    velocity.x *= 0.9
    velocity.z *= 0.9
    super.tick(collisionDetector)
  }

  override def fromNBT(tag: CompoundTag): Unit = {
    super.fromNBT(tag)
    NBTUtil.getCompoundTag(tag, "ai").foreach(aiTag => ai.fromNBT(aiTag))
  }

  override def toNBT: Seq[Tag[_]] = super.toNBT :+ new StringTag("type", "sheep") :+ NBTUtil.makeCompoundTag("ai", ai.toNBT)
}

object SheepEntity {
  def atStartPos(pos: CylCoords, world: BlocksInWorld, aiFactory: EntityAIFactory[SheepEntity], model: EntityModel)(implicit cylSizeImpl: CylinderSize): SheepEntity = {
    val pl = new SheepEntity(model, world, aiFactory)
    pl.position = pos
    pl
  }
}
