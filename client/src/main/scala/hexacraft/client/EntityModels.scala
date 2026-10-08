package hexacraft.client

import hexacraft.client.entity.*
import hexacraft.world.entity.Entity

/** The models are only needed by the client, so entities are decoded without them and get them from here. */
object EntityModels {
  def forType(entityType: String): Option[ModelComponent] = entityType match {
    case "player" =>
      val model = PlayerEntityModel.create()
      Some(ModelComponent(model, PlayerEntityModel.skin, PlayerAnimation(model)))
    case "sheep" =>
      val model = SheepEntityModel.create()
      Some(ModelComponent(model, SheepEntityModel.skin, SheepAnimation(model)))
    case "boat" =>
      Some(ModelComponent(BoatEntityModel.create(), BoatEntityModel.skin, EntityAnimation.none))
    case _ => None
  }

  def addModel(entity: Entity): Entity =
    forType(entity.typeName).map(entity.withComponent).getOrElse(entity)
}
