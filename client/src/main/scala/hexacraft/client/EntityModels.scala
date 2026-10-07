package hexacraft.client

import hexacraft.client.entity.{BoatEntityModel, PlayerEntityModel, SheepEntityModel}
import hexacraft.world.entity.{Entity, EntityModel, ModelComponent}

/** The models are only needed by the client, so entities are decoded without them and get them from here. */
object EntityModels {
  def forType(entityType: String): Option[EntityModel] = entityType match {
    case "player" => Some(PlayerEntityModel.create("player"))
    case "sheep"  => Some(SheepEntityModel.create("sheep"))
    case "boat"   => Some(BoatEntityModel.create("boat"))
    case _        => None
  }

  def addModel(entity: Entity): Entity =
    forType(entity.typeName).map(m => entity.withComponent(ModelComponent(m))).getOrElse(entity)
}
