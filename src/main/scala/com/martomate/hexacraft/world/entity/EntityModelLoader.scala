package com.martomate.hexacraft.world.entity

import com.martomate.hexacraft.infra.fs.FileUtils
import com.martomate.hexacraft.world.block.HexBox
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.entity.EntityModel
import com.martomate.hexacraft.world.entity.base.BasicEntityModel
import com.martomate.hexacraft.world.entity.player.PlayerEntityModel
import com.martomate.hexacraft.world.entity.sheep.SheepEntityModel

import com.eclipsesource.json.{Json, JsonObject}
import com.martomate.hexacraft.world.CylinderSize

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
