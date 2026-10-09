package hexacraft.client

import hexacraft.client.entity.*
import hexacraft.world.entity.EntityModel

/** Decides how each entity type looks (its skin and animation). The shape of the entity (the model) is sent by the
  * server, and the skin and animation refer to its parts by name.
  */
object EntityAppearances {

  /** Returns None if the client doesn't know how to render the given entity type */
  def modelComponent(entityType: String, model: EntityModel): Option[ModelComponent] = {
    val pose = EntityPose(model)
    entityType match {
      case "player" => Some(ModelComponent(pose, EntitySkins.player, PlayerAnimation(pose)))
      case "sheep"  => Some(ModelComponent(pose, EntitySkins.sheep, SheepAnimation(pose)))
      case "boat"   => Some(ModelComponent(pose, EntitySkins.boat, EntityAnimation.none))
      case _        => None
    }
  }
}
