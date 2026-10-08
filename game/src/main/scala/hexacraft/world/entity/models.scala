package hexacraft.world.entity

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

object EntityModel {

  /** The size of a model pixel in world units (1/32 of a block) */
  val pixelSize: Double = 1.0 / 64
}

/** A hexagonal prism, with sizes in model pixels. `radius` is the big radius of the hexagon. */
case class HexPrism(radius: Int, length: Int) {
  def isEmpty: Boolean = radius == 0 || length == 0
}

object HexPrism {
  val empty: HexPrism = HexPrism(0, 0)
}

/** A part of an entity model. All lengths are in model pixels.
  *
  * The part is attached to its parent at `position`, and rotated by `rotation` around that point.
  * Rotations are applied in the order z, x, y.
  * The `prismOffset` can be used to move the rotation point along the prism.
  */
class EntityPart(
    val name: String,
    val prism: HexPrism,
    _position: Vector3fc,
    _rotation: Vector3fc,
    val parent: Option[String] = None,
    val prismOffset: Float = 0
) {
  val position: Vector3fc = new Vector3f(_position)
  val rotation: Vector3fc = new Vector3f(_rotation)

  /** Pivot parts (with an empty prism) are not rendered */
  def isVisible: Boolean = !prism.isEmpty
}
