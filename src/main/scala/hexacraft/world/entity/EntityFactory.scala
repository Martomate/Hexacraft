package hexacraft.world.entity

import hexacraft.world.CylinderSize
import hexacraft.world.coord.fp.CylCoords

import com.flowpowered.nbt.CompoundTag

trait EntityFactory:
  def atStartPos(pos: CylCoords)(using CylinderSize): Entity

  def fromNBT(tag: CompoundTag)(using CylinderSize): Entity
