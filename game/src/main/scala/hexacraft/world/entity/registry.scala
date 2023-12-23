package hexacraft.world.entity

import hexacraft.world.CylinderSize
import hexacraft.world.coord.fp.CylCoords

import com.martomate.nbt.Nbt

trait EntityFactory:
  def atStartPos(pos: CylCoords)(using CylinderSize): Entity

  def fromNBT(tag: Nbt.MapTag)(using CylinderSize): Entity

trait EntityRegistry {
  def get(name: String): Option[EntityFactory]
}

object EntityRegistry {
  def empty: EntityRegistry = _ => None

  def from(mappings: Map[String, EntityFactory]): EntityRegistry = name => mappings.get(name)
}
