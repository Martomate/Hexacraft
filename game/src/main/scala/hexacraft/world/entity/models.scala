package hexacraft.world.entity

import hexacraft.world.HexBox
import hexacraft.world.coord.CylCoords

import org.joml.{Matrix4f, Vector3d, Vector3dc, Vector3f}

/** An entity model is a collection of hexagonal prisms (parts).
  *
  * Each part is placed relative to its parent part (if any). A part with an empty box is not rendered, it only serves
  * as a pivot for its children.
  */
class EntityModel(val textureName: String, val parts: Seq[EntityPart]) {
  private val partsByName: Map[String, EntityPart] = parts.map(p => p.name -> p).toMap
  require(partsByName.size == parts.size, s"Part names must be unique")

  def part(name: String): EntityPart = {
    partsByName.getOrElse(name, throw new IllegalArgumentException(s"The model has no part named '$name'"))
  }
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

class EntityPart(
    val name: String,
    val box: HexBox,
    pos: CylCoords.Offset,
    val rotation: Vector3f,
    textureBaseOffset: (Int, Int) = (0, 0),
    val parent: Option[EntityPart] = None
) {
  private val boxRadius = (box.radius * 32 / 0.5f).round
  private val boxHeight = ((box.top - box.bottom) * 32 / 0.5f).round

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

  def texture(side: Int): Int = {
    val offset = if side < 2 then 0x12345 else 0
    val texID = 4
    offset << 12 | texID
  }

  def textureOffset(side: Int): (Int, Int) = {
    val (dx, dy) = side match {
      case 0 => (0, 0)
      case 1 => (0, boxRadius + boxHeight)
      case _ => ((side - 2) * boxRadius, boxRadius)
    }

    val (sx, sy) = textureBaseOffset
    (sx + dx, sy + dy)
  }

  def textureSize(side: Int): (Int, Int) = {
    if side < 2 then {
      (boxRadius, boxRadius)
    } else {
      (boxRadius, boxHeight)
    }
  }
}
