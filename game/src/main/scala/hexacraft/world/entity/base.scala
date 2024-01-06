package hexacraft.world.entity

import hexacraft.util.Result
import hexacraft.util.Result.{Err, Ok}
import hexacraft.world.{CylinderSize, HexBox}
import hexacraft.world.coord.CylCoords

import com.martomate.nbt.Nbt
import org.joml.{Matrix4f, Vector3d}

class Entity(
    val typeName: String,
    val data: EntityBaseData,
    val model: Option[EntityModel],
    val ai: Option[EntityAI],
    val boundingBox: HexBox
) {
  def position: CylCoords = data.position
  def rotation: Vector3d = data.rotation
  def velocity: Vector3d = data.velocity

  def transform: Matrix4f = data.transform

  def toNBT: Nbt.MapTag =
    val dataNbt = data.toNBT

    Nbt
      .makeMap(
        "type" -> Nbt.StringTag(typeName),
        "pos" -> dataNbt.pos,
        "velocity" -> dataNbt.velocity,
        "rotation" -> dataNbt.rotation
      )
      .withOptionalField("ai", ai.map(_.toNBT))
}

object EntityFactory:
  val playerBounds = new HexBox(0.2f, 0, 1.75f)
  private val sheepBounds = new HexBox(0.4f, 0, 0.75f)

  def atStartPos(pos: CylCoords, entityType: String)(using CylinderSize): Result[Entity, String] =
    entityType match
      case "player" =>
        val model = PlayerEntityModel.create("player")
        Ok(Entity("player", new EntityBaseData(pos), Some(model), Some(SimpleWalkAI.create), playerBounds))

      case "sheep" =>
        val model = SheepEntityModel.create("sheep")
        Ok(Entity("sheep", new EntityBaseData(pos), Some(model), Some(SimpleWalkAI.create), sheepBounds))

      case _ => Err(s"Entity-type '$entityType' not found")

  def fromNbt(tag: Nbt.MapTag)(using CylinderSize): Result[Entity, String] =
    val entType = tag.getString("type", "")

    entType match
      case "player" =>
        val model = PlayerEntityModel.create("player")
        val baseData = EntityBaseData.fromNBT(tag)
        val ai: EntityAI =
          tag.getMap("ai") match
            case Some(t) => SimpleWalkAI.fromNBT(t)
            case None    => SimpleWalkAI.create

        Ok(Entity("player", baseData, Some(model), Some(ai), playerBounds))

      case "sheep" =>
        val model = SheepEntityModel.create("sheep")
        val baseData = EntityBaseData.fromNBT(tag)
        val ai: EntityAI =
          tag.getMap("ai") match
            case Some(t) => SimpleWalkAI.fromNBT(t)
            case None    => SimpleWalkAI.create
        Ok(Entity("sheep", baseData, Some(model), Some(ai), sheepBounds))

      case _ => Err(s"Entity-type '$entType' not found")

class EntityBaseData(
    var position: CylCoords,
    var rotation: Vector3d = new Vector3d,
    var velocity: Vector3d = new Vector3d
):
  def transform: Matrix4f = new Matrix4f()
    .translate(position.toVector3f)
    .rotateZ(rotation.z.toFloat)
    .rotateX(rotation.x.toFloat)
    .rotateY(rotation.y.toFloat)

  def toNBT: EntityBaseData.NbtData = EntityBaseData.NbtData(
    pos = Nbt.makeVectorTag(position.toVector3d),
    velocity = Nbt.makeVectorTag(velocity),
    rotation = Nbt.makeVectorTag(rotation)
  )

object EntityBaseData:
  case class NbtData(pos: Nbt.MapTag, velocity: Nbt.MapTag, rotation: Nbt.MapTag)

  def fromNBT(tag: Nbt.MapTag)(using CylinderSize): EntityBaseData =
    val position = tag
      .getMap("pos")
      .map(t => CylCoords(t.setVector(new Vector3d)))
      .getOrElse(CylCoords(0, 0, 0))

    val data = new EntityBaseData(position = position)
    tag.getMap("velocity").foreach(t => t.setVector(data.velocity))
    tag.getMap("rotation").foreach(t => t.setVector(data.rotation))
    data
