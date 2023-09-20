package hexacraft.world.entity.base

import hexacraft.world.block.HexBox
import hexacraft.world.coord.fp.CylCoords
import hexacraft.world.entity.EntityPart

import org.joml.{Matrix4f, Vector3f}

class BasicEntityPart(
    override val box: HexBox,
    pos: CylCoords.Offset,
    val rotation: Vector3f,
    textureBaseOffset: (Int, Int) = (0, 0),
    parentPart: EntityPart = null
) extends EntityPart {
  private val boxRadius = (box.radius * 32 / 0.5f).round
  private val boxHeight = ((box.top - box.bottom) * 32 / 0.5f).round

  override def baseTransform: Matrix4f =
    val m = if parentPart != null then Matrix4f(parentPart.baseTransform) else Matrix4f()

    m
      .translate(pos.toVector3f)
      .rotateZ(rotation.z)
      .rotateX(rotation.x)
      .rotateY(rotation.y)
      .translate(0, box.bottom, 0)

  override def transform: Matrix4f = baseTransform
    .scale(new Vector3f(box.radius, box.top - box.bottom, box.radius))

  override def texture(side: Int): Int = {
    val offset = if (side < 2) 0x12345 else 0
    val texID = 4
    offset << 12 | texID
  }

  override def textureOffset(side: Int): (Int, Int) = {
    val add = side match {
      case 0 => (0, 0)
      case 1 => (0, boxRadius + boxHeight)
      case _ => ((side - 2) * boxRadius, boxRadius)
    }
    (textureBaseOffset._1 + add._1, textureBaseOffset._2 + add._2)
  }

  override def textureSize(side: Int): (Int, Int) =
    if (side < 2) (boxRadius, boxRadius)
    else (boxRadius, boxHeight)
}
