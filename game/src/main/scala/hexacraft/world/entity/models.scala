package hexacraft.world.entity

import hexacraft.world.HexBox
import hexacraft.world.coord.CylCoords

import org.joml.{Matrix4f, Vector3f}

/** An entity model is a collection of hexagonal prisms (parts).
  *
  * Each part is placed relative to its parent part (if any). A part with an empty box is not rendered, it only serves
  * as a pivot for its children.
  */
class EntityModel(val parts: Seq[EntityPart]) {
  private val partsByName: Map[String, EntityPart] = parts.map(p => p.name -> p).toMap
  require(partsByName.size == parts.size, "Part names must be unique")

  def part(name: String): EntityPart = {
    partsByName.getOrElse(name, throw new IllegalArgumentException(s"The model has no part named '$name'"))
  }
}

class EntityPart(
    val name: String,
    val box: HexBox,
    pos: CylCoords.Offset,
    val rotation: Vector3f,
    val parent: Option[EntityPart] = None
) {

  /** Pivot parts (with an empty box) are not rendered */
  def isVisible: Boolean = box.radius > 0 && box.top > box.bottom

  def baseTransform: Matrix4f = {
    val base = parent.map(p => Matrix4f(p.baseTransform)).getOrElse(Matrix4f())

    base
      .translate(pos.toVector3f)
      .rotateZ(rotation.z)
      .rotateX(rotation.x)
      .rotateY(rotation.y)
      .translate(0, box.bottom, 0)
  }

  def transform: Matrix4f = {
    baseTransform.scale(
      new Vector3f(
        box.radius,
        box.top - box.bottom,
        box.radius
      )
    )
  }
}
