package com.martomate.hexacraft.world.entity.sheep

import com.martomate.hexacraft.util.{CylinderSize, Nbt, NBTUtil}
import com.martomate.hexacraft.world.block.Blocks
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.entity.{EntityBaseData, EntityFactory, EntityModelLoader}
import com.martomate.hexacraft.world.entity.ai.{EntityAI, SimpleWalkAI}

import com.flowpowered.nbt.CompoundTag

class SheepFactory(using modelLoader: EntityModelLoader) extends EntityFactory:
  override def atStartPos(pos: CylCoords)(using CylinderSize, Blocks): SheepEntity =
    val model = modelLoader.load("sheep")
    new SheepEntity(model, new EntityBaseData(position = pos), SimpleWalkAI.create)

  override def fromNBT(tag: CompoundTag)(using CylinderSize, Blocks): SheepEntity =
    val model = modelLoader.load("sheep")
    val baseData = EntityBaseData.fromNBT(tag)
    val ai: EntityAI =
      NBTUtil.getCompoundTag(Nbt.from(tag), "ai") match
        case Some(t) => SimpleWalkAI.fromNBT(t.toCompoundTag("ai"))
        case None    => SimpleWalkAI.create
    new SheepEntity(model, baseData, ai)
