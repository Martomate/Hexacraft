package com.martomate.hexacraft.world.entity.sheep

import com.flowpowered.nbt.{CompoundTag, StringTag, Tag}
import com.martomate.hexacraft.util.NBTUtil
import com.martomate.hexacraft.world.block.HexBox
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.entity.EntityModel
import com.martomate.hexacraft.world.entity.ai.{EntityAI, EntityAIFactory}
import com.martomate.hexacraft.world.entity.base.BasicEntity
import com.martomate.hexacraft.world.entity.sheep.ai.SimpleSheepAIInput
import com.martomate.hexacraft.world.worldlike.IWorld

class SheepEntity(_model: EntityModel, _world: IWorld, aiFactory: EntityAIFactory[SheepEntity]) extends BasicEntity(_model, _world)(_world.size) {
  override val boundingBox: HexBox = new HexBox(0.2f, 0, 1.75f)

  override def id: String = "sheep"

  private val ai: EntityAI = aiFactory.makeEntityAI(_world, this)

  override def tick(): Unit = {
    ai.tick()
    velocity.add(ai.acceleration())

    velocity.x *= 0.9
    velocity.z *= 0.9
    super.tick()
  }

  override def fromNBT(tag: CompoundTag): Unit = {
    super.fromNBT(tag)
    NBTUtil.getCompoundTag(tag, "ai").foreach(aiTag => ai.fromNBT(aiTag))
  }

  override def toNBT: Seq[Tag[_]] = super.toNBT :+ new StringTag("type", "sheep") :+ NBTUtil.makeCompoundTag("ai", ai.toNBT)
}

object SheepEntity {
  def atStartPos(pos: CylCoords, world: IWorld, aiFactory: EntityAIFactory[SheepEntity], model: EntityModel): SheepEntity = {
    val pl = new SheepEntity(model, world, aiFactory)
    pl.position = pos
    pl
  }
}
