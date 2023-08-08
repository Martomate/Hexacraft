package hexacraft.world.entity

import hexacraft.world.CylinderSize
import hexacraft.world.block.Blocks
import hexacraft.world.coord.fp.CylCoords

import com.flowpowered.nbt.CompoundTag

trait EntityFactory:
  def atStartPos(pos: CylCoords)(using CylinderSize, Blocks): Entity

  def fromNBT(tag: CompoundTag)(using CylinderSize, Blocks): Entity
