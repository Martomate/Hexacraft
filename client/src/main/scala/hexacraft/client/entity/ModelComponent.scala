package hexacraft.client.entity

import hexacraft.world.entity.{EntityComponent, EntityModel}

import org.joml.{Vector3d, Vector3dc}

/** Everything the client needs to render an entity. The server knows nothing about this. */
class ModelComponent(val pose: EntityPose, val skin: EntitySkin, val animation: EntityAnimation)
    extends EntityComponent {
  skin.partNames.foreach(model.part) // fails if the skin refers to a part that the model doesn't have

  def model: EntityModel = pose.model
}

/** Moves the parts of a model based on what the entity is doing */
trait EntityAnimation {

  /** @param rotation
    *   the rotation of the entity (i.e. the direction it is looking)
    * @param mountRotation
    *   the rotation of the entity this entity is sitting on, if any
    */
  def tick(
      walking: Boolean,
      headDirection: Option[Vector3d],
      rotation: Vector3dc,
      mountRotation: Option[Vector3dc]
  ): Unit
}

object EntityAnimation {
  val none: EntityAnimation = (_, _, _, _) => ()
}
