package com.martomate.hexacraft.world.entity.player

import com.martomate.hexacraft.util.{CylinderSize, NBTUtil}
import com.martomate.hexacraft.world.block.Blocks
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.entity.{EntityBaseData, EntityFactory, EntityModelLoader}
import com.martomate.hexacraft.world.entity.ai.{EntityAI, SimpleWalkAI}

import com.flowpowered.nbt.CompoundTag

class PlayerFactory(using EntityModelLoader) extends EntityFactory:
  override def atStartPos(pos: CylCoords)(using CylinderSize, Blocks): PlayerEntity =
    val model = summon[EntityModelLoader].load("player")
    new PlayerEntity(model, new EntityBaseData(position = pos), SimpleWalkAI.create)

  override def fromNBT(tag: CompoundTag)(using CylinderSize, Blocks): PlayerEntity =
    val model = summon[EntityModelLoader].load("player")
    val baseData = EntityBaseData.fromNBT(tag)
    val ai: EntityAI =
      NBTUtil.getCompoundTag(tag, "ai") match
        case Some(t) => SimpleWalkAI.fromNBT(t)
        case None    => SimpleWalkAI.create

    new PlayerEntity(model, baseData, ai)
