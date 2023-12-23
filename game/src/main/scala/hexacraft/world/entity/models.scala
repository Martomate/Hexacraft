package hexacraft.world.entity

import hexacraft.renderer.TextureSingle
import hexacraft.world.coord.CylCoords
import hexacraft.world.HexBox

import org.joml.{Matrix4f, Vector3f}

trait EntityModel {
  def parts: Seq[EntityPart]
  def texture: TextureSingle
  def tick(): Unit
}

trait EntityPart {
  def baseTransform: Matrix4f
  def transform: Matrix4f
  def box: HexBox
  def texture(side: Int): Int
  def textureOffset(side: Int): (Int, Int) = (0, 0)
  def textureSize(side: Int): (Int, Int)
}

class EntityModelLoader {

  def load(name: String): EntityModel = name match {
    case "player" => PlayerEntityModel.create("player")
    case "sheep"  => SheepEntityModel.create("sheep")
    case _        => BasicEntityModel.create(CylCoords.Offset(0, 0, 0), HexBox(0, 0, 0))
  }
}

object BasicEntityModel:
  def create(pos: CylCoords.Offset, box: HexBox): BasicEntityModel =
    new BasicEntityModel(pos, box)

class BasicEntityModel(pos: CylCoords.Offset, box: HexBox) extends EntityModel {
  private val theBox = new BasicEntityPart(box, pos, new Vector3f)

  override val parts: Seq[EntityPart] = Seq(theBox)

  override def tick(): Unit = ()

  override def texture: TextureSingle = ???
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
