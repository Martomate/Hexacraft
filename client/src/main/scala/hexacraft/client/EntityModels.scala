package hexacraft.client

import hexacraft.client.entity.*
import hexacraft.world.entity.{Entity, EntityAnimation, ModelComponent}

/** The models are only needed by the client, so entities are decoded without them and get them from here. */
object EntityModels {
  def forType(entityType: String): Option[ModelComponent] = entityType match {
    case "player" =>
      val model = PlayerEntityModel.create("player")
      Some(ModelComponent(model, PlayerAnimation(model)))
    case "sheep" =>
      val model = SheepEntityModel.create("sheep")
      Some(ModelComponent(model, SheepAnimation(model)))
    case "boat" =>
      Some(ModelComponent(BoatEntityModel.create("boat"), EntityAnimation.none))
    case _ => None
  }

  def addModel(entity: Entity): Entity =
    forType(entity.typeName).map(entity.withComponent).getOrElse(entity)
}
