package hexacraft.server.world

import hexacraft.server.entity.EntityModels
import hexacraft.world.entity.EntityModel

import org.joml.{Matrix4d, Matrix4dc, Vector3d, Vector3dc}

import java.util.concurrent.ConcurrentHashMap

/** The corners of the prism of a part, relative to the entity, in world units.
  *
  * @param top
  *   the 6 corners at the end of the prism (towards the part's positive y-axis), at angles 0, 60, 120, ... degrees
  * @param bottom
  *   the 6 corners at the start of the prism, in the same order
  */
case class PrismCorners(top: IndexedSeq[Vector3dc], bottom: IndexedSeq[Vector3dc])

/** The parts of an entity model placed relative to the entity, in world units, with the model in its resting pose.
  *
  * This must match how the client places the parts (in EntityPose) when it renders the model.
  */
class PlacedModel(val model: EntityModel) {

  /** The transform of each part's attachment point relative to the entity */
  val partTransforms: IndexedSeq[Matrix4dc] = {
    val px = EntityModel.pixelSize
    val result = new Array[Matrix4d](model.parts.size)

    for idx <- model.parts.indices do {
      val part = model.parts(idx)
      val parentIdx = model.parentIndices(idx)

      // parents come before their children, so the parent's transform is already calculated
      result(idx) = (if parentIdx != -1 then Matrix4d(result(parentIdx)) else Matrix4d())
        .translate(part.position.x * px, part.position.y * px, part.position.z * px)
        .rotateZ(part.rotation.z)
        .rotateX(part.rotation.x)
        .rotateY(part.rotation.y)
    }

    result.toIndexedSeq
  }

  /** The corners of the prisms of all visible parts */
  val prismCorners: IndexedSeq[PrismCorners] = {
    val px = EntityModel.pixelSize

    for {
      idx <- model.parts.indices
      part = model.parts(idx)
      if part.isVisible
    } yield {
      val radius = part.prism.radius * px
      val bottom = part.prismOffset * px
      val top = bottom + part.prism.length * px

      def ring(y: Double) = (0 until 6).map { i =>
        val angle = i * math.Pi / 3
        val corner: Vector3dc = partTransforms(idx).transformPosition(
          new Vector3d(radius * math.cos(angle), y, radius * math.sin(angle))
        )
        corner
      }

      PrismCorners(ring(top), ring(bottom))
    }
  }
}

object PlacedModel {
  private val placedModelsByType = new ConcurrentHashMap[String, PlacedModel]()

  /** The placed model of the given entity type (computed once per type), or None if there is no model */
  def forType(entityType: String): Option[PlacedModel] = {
    EntityModels
      .forType(entityType)
      .map(model => placedModelsByType.computeIfAbsent(entityType, _ => PlacedModel(model)))
  }
}
