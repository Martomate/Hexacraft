package hexacraft.client

import hexacraft.client.entity.*
import hexacraft.world.entity.Entity

/** The models are only needed by the client, so entities are decoded without them and get them from here. */
object EntityModels {
  def forType(entityType: String): Option[ModelComponent] = entityType match {
    case "player" =>
      val pose = EntityPose(PlayerEntityModel.model)
      Some(ModelComponent(pose, PlayerEntityModel.skin, PlayerAnimation(pose)))
    case "sheep" =>
      val pose = EntityPose(SheepEntityModel.model)
      Some(ModelComponent(pose, SheepEntityModel.skin, SheepAnimation(pose)))
    case "boat" =>
      val pose = EntityPose(BoatEntityModel.model)
      Some(ModelComponent(pose, BoatEntityModel.skin, EntityAnimation.none))
    case _ => None
  }

  def addModel(entity: Entity): Entity =
    forType(entity.typeName).map(entity.withComponent).getOrElse(entity)
}
