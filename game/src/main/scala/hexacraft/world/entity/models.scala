package hexacraft.world.entity

import hexacraft.world.HexBox
import hexacraft.world.coord.CylCoords

import org.joml.{Vector3f, Vector3fc}

/** An entity model is a collection of hexagonal prisms (parts).
  * It is immutable, so it can be shared by all entities of the same type.
  * Each part is placed relative to its parent part (if any).
  * Parents have to come before their children in `parts`.
  * A part with an empty box is not rendered, it only serves as a pivot for its children.
  */
class EntityModel(val parts: IndexedSeq[EntityPart]) {
  private val indicesByName: Map[String, Int] = parts.map(_.name).zipWithIndex.toMap
  require(indicesByName.size == parts.size, "Part names must be unique")

  /** The index of each part's parent, or -1 if the part has no parent */
  val parentIndices: IndexedSeq[Int] = parts.zipWithIndex.map { (part, idx) =>
    part.parent match {
      case Some(parent) =>
        val parentIdx = indicesByName.getOrElse(parent, -1)
        require(parentIdx != -1, s"The parent of '${part.name}' ('$parent') is not in the model")
        require(parentIdx < idx, s"The parent of '${part.name}' has to come before it")
        parentIdx
      case None => -1
    }
  }

  def indexOf(name: String): Int = {
    indicesByName.getOrElse(name, throw new IllegalArgumentException(s"The model has no part named '$name'"))
  }

  def part(name: String): EntityPart = parts(indexOf(name))
}

/** A hexagonal prism placed at `position` relative to its parent, and rotated by `rotation` around that point. */
class EntityPart(
    val name: String,
    val box: HexBox,
    val position: CylCoords.Offset,
    _rotation: Vector3fc,
    val parent: Option[String] = None
) {
  val rotation: Vector3fc = new Vector3f(_rotation)

  /** Pivot parts (with an empty box) are not rendered */
  def isVisible: Boolean = box.radius > 0 && box.top > box.bottom
}
