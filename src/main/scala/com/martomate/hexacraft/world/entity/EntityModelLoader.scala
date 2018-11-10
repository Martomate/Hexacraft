package com.martomate.hexacraft.world.entity

import com.eclipsesource.json.{Json, JsonObject}
import com.martomate.hexacraft.util.{CylinderSize, FileUtils}
import com.martomate.hexacraft.world.block.HexBox
import com.martomate.hexacraft.world.coord.fp.CylCoords

class EntityModelLoader(basePath: String = "spec/entities")(implicit cylinderSize: CylinderSize) {
  private def makeEntity(name: String, setup: JsonObject): EntityModel = name match {
    case "player" => new PlayerEntityModel(setup)
    case _ => new TempEntityModel(CylCoords(0, 0, 0), new HexBox(0, 0, 0))
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
