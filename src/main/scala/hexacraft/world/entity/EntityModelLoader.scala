package hexacraft.world.entity

import com.eclipsesource.json.{Json, JsonObject}
import hexacraft.infra.fs.FileUtils
import hexacraft.world.CylinderSize
import hexacraft.world.block.HexBox
import hexacraft.world.coord.fp.CylCoords
import hexacraft.world.entity.EntityModel
import hexacraft.world.entity.base.BasicEntityModel
import hexacraft.world.entity.player.PlayerEntityModel
import hexacraft.world.entity.sheep.SheepEntityModel

class EntityModelLoader(basePath: String = "spec/entities") {
  private def makeEntity(name: String, setup: JsonObject): EntityModel = name match {
    case "player" => PlayerEntityModel.fromJson(setup)
    case "sheep"  => SheepEntityModel.fromJson(setup)
    case _        => BasicEntityModel.create(CylCoords.Offset(0, 0, 0), new HexBox(0, 0, 0))
  }

  def load(name: String): EntityModel = {
    FileUtils.getResourceFile(s"$basePath/$name.json") match {
      case Some(file) =>
        val reader = FileUtils.getBufferedReader(file)
        if (reader != null) {
          val base = Json.parse(reader).asObject()
          val name = base.get("texture").asString()
          val entity = makeEntity(name, base)
          entity
        } else makeEntity("", null)
      case None =>
        makeEntity("", null)
    }
  }
}
