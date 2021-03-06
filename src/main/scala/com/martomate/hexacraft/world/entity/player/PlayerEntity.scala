package com.martomate.hexacraft.world.entity.player

import com.flowpowered.nbt.{CompoundTag, StringTag, Tag}
import com.martomate.hexacraft.util.NBTUtil
import com.martomate.hexacraft.world.block.HexBox
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.entity.EntityModel
import com.martomate.hexacraft.world.entity.ai.{EntityAI, EntityAIFactory}
import com.martomate.hexacraft.world.entity.base.BasicEntity
import com.martomate.hexacraft.world.worldlike.IWorld

class PlayerEntity(_model: EntityModel, _world: IWorld, aiFactory: EntityAIFactory[PlayerEntity]) extends BasicEntity(_model, _world)(_world.size) {
  override val boundingBox: HexBox = new HexBox(0.2f, 0, 1.75f)

  override def id: String = "player"

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

  override def toNBT: Seq[Tag[_]] = super.toNBT :+ new StringTag("type", "player") :+ NBTUtil.makeCompoundTag("ai", ai.toNBT)
}

object PlayerEntity {
  def atStartPos(pos: CylCoords, world: IWorld, aiFactory: EntityAIFactory[PlayerEntity], model: EntityModel): PlayerEntity = {
    val pl = new PlayerEntity(model, world, aiFactory)
    pl.position = pos
    pl
  }
}
