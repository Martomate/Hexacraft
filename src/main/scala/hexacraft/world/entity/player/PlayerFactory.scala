package hexacraft.world.entity.player

import hexacraft.nbt.{Nbt, NBTUtil}
import hexacraft.world.CylinderSize
import hexacraft.world.block.Blocks
import hexacraft.world.coord.fp.CylCoords
import hexacraft.world.entity.{EntityBaseData, EntityFactory, EntityModel, EntityModelLoader}
import hexacraft.world.entity.ai.{EntityAI, SimpleWalkAI}

import com.flowpowered.nbt.CompoundTag

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
