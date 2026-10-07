package hexacraft.world.entity

import hexacraft.world.HexBox
import hexacraft.world.coord.CylCoords

import org.joml.{Matrix4f, Vector3d, Vector3f}

trait EntityModel {
  def parts: Seq[EntityPart]
  def textureName: String
  def tick(walking: Boolean, headDirection: Option[Vector3d]): Unit
}

trait EntityPart {
  def baseTransform: Matrix4f
  def transform: Matrix4f
  def box: HexBox
  def texture(side: Int): Int
  def textureOffset(side: Int): (Int, Int) = (0, 0)
  def textureSize(side: Int): (Int, Int)
}

class BasicEntityPart(
    override val box: HexBox,
    pos: CylCoords.Offset,
    val rotation: Vector3f,
    textureBaseOffset: (Int, Int) = (0, 0),
    parentPart: EntityPart = null
) extends EntityPart {
  private val boxRadius = (box.radius * 32 / 0.5f).round
  private val boxHeight = ((box.top - box.bottom) * 32 / 0.5f).round

  override def baseTransform: Matrix4f = {
    val base = if parentPart != null then Matrix4f(parentPart.baseTransform) else Matrix4f()

    base
      .translate(pos.toVector3f)
      .rotateZ(rotation.z)
      .rotateX(rotation.x)
      .rotateY(rotation.y)
      .translate(0, box.bottom, 0)
  }

  override def transform: Matrix4f = {
    baseTransform.scale(
      new Vector3f(
        box.radius,
        box.top - box.bottom,
        box.radius
      )
    )
  }

  override def texture(side: Int): Int = {
    val offset = if side < 2 then 0x12345 else 0
    val texID = 4
    offset << 12 | texID
  }

  override def textureOffset(side: Int): (Int, Int) = {
    val (dx, dy) = side match {
      case 0 => (0, 0)
      case 1 => (0, boxRadius + boxHeight)
      case _ => ((side - 2) * boxRadius, boxRadius)
    }

    val (sx, sy) = textureBaseOffset
    (sx + dx, sy + dy)
  }

  override def textureSize(side: Int): (Int, Int) = {
    if side < 2 then {
      (boxRadius, boxRadius)
    } else {
      (boxRadius, boxHeight)
    }
  }
}
