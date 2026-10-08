package hexacraft.client.entity

import hexacraft.world.entity.EntityModel

import org.joml.{Matrix4f, Vector3f}

/** The current pose of an entity, i.e. how much each part of the model is rotated away from its resting rotation. */
class EntityPose(val model: EntityModel) {
  private val rotations = Array.fill(model.parts.size)(new Vector3f)

  /** The (mutable) pose rotation of the given part */
  def rotation(partName: String): Vector3f = rotations(model.indexOf(partName))

  /** The transform of each part relative to the entity (excluding the scaling of the part's box) */
  def partTransforms: Array[Matrix4f] = {
    val parts = model.parts
    val result = new Array[Matrix4f](parts.size)

    for idx <- parts.indices do {
      val part = parts(idx)
      val parentIdx = model.parentIndices(idx)
      val pose = rotations(idx)
      val rest = part.rotation

      // parents come before their children, so the parent's transform is already calculated
      result(idx) = (if parentIdx != -1 then Matrix4f(result(parentIdx)) else Matrix4f())
        .translate(part.position.toVector3f)
        .rotateZ(pose.z)
        .rotateX(pose.x)
        .rotateY(pose.y)
        .rotateZ(rest.z)
        .rotateX(rest.x)
        .rotateY(rest.y)
        .translate(0, part.box.bottom, 0)
    }

    result
  }
}
