package hexacraft.world.entity

import hexacraft.nbt.Nbt
import hexacraft.world.CylinderSize
import hexacraft.world.coord.fp.CylCoords

trait EntityFactory:
  def atStartPos(pos: CylCoords)(using CylinderSize): Entity

  def fromNBT(tag: Nbt.MapTag)(using CylinderSize): Entity
