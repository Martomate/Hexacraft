package hexacraft.world.entity

import hexacraft.world.CylinderSize
import hexacraft.world.coord.CylCoords

import com.martomate.nbt.Nbt

trait EntityFactory:
  def atStartPos(pos: CylCoords)(using CylinderSize): Entity

  def fromNBT(tag: Nbt.MapTag)(using CylinderSize): Entity

class EntityRegistry {
  val player: PlayerFactory = new PlayerFactory(() => PlayerEntityModel.create("player"))
  val sheep: SheepFactory = new SheepFactory(() => SheepEntityModel.create("sheep"))

  def get(name: String): Option[EntityFactory] =
    name match
      case "player" => Some(player)
      case "sheep"  => Some(sheep)
      case _        => None
}
