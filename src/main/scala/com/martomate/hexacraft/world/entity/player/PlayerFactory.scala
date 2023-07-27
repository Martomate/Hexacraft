package com.martomate.hexacraft.world.entity.player

import com.flowpowered.nbt.CompoundTag
import com.martomate.hexacraft.nbt.{Nbt, NBTUtil}
import com.martomate.hexacraft.world.CylinderSize
import com.martomate.hexacraft.world.block.Blocks
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.entity.{EntityBaseData, EntityFactory, EntityModel, EntityModelLoader}
import com.martomate.hexacraft.world.entity.ai.{EntityAI, SimpleWalkAI}

class PlayerFactory(makeModel: () => EntityModel) extends EntityFactory:
  override def atStartPos(pos: CylCoords)(using CylinderSize, Blocks): PlayerEntity =
    val model = makeModel()
    new PlayerEntity(model, new EntityBaseData(position = pos), SimpleWalkAI.create)

  override def fromNBT(tag: CompoundTag)(using CylinderSize, Blocks): PlayerEntity =
    val model = makeModel()
    val baseData = EntityBaseData.fromNBT(tag)
    val ai: EntityAI =
      Nbt.from(tag).getCompoundTag("ai") match
        case Some(t) => SimpleWalkAI.fromNBT(t)
        case None    => SimpleWalkAI.create

    new PlayerEntity(model, baseData, ai)
