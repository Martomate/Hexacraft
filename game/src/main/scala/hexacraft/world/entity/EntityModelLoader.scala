package hexacraft.world.entity

import hexacraft.world.block.HexBox
import hexacraft.world.coord.fp.CylCoords
import hexacraft.world.entity.base.BasicEntityModel
import hexacraft.world.entity.player.PlayerEntityModel
import hexacraft.world.entity.sheep.SheepEntityModel

class EntityModelLoader {

  def load(name: String): EntityModel = name match {
    case "player" => PlayerEntityModel.create("player")
    case "sheep"  => SheepEntityModel.create("sheep")
    case _        => BasicEntityModel.create(CylCoords.Offset(0, 0, 0), HexBox(0, 0, 0))
  }
}
