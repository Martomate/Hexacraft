package hexacraft.server.entity

import hexacraft.world.entity.EntityModel

/** The shapes of all entity types. The server sends them to the clients, which decide how to render them. */
object EntityModels {
  def forType(entityType: String): Option[EntityModel] = entityType match {
    case "player" => Some(PlayerEntityModel.model)
    case "sheep"  => Some(SheepEntityModel.model)
    case "boat"   => Some(BoatEntityModel.model)
    case _        => None
  }
}
