package hexacraft.world.entity

import hexacraft.util.Result
import hexacraft.util.Result.{Err, Ok}
import hexacraft.world.{BlocksInWorld, CollisionDetector, CylinderSize, HexBox}

import com.martomate.nbt.Nbt
import hexacraft.world.coord.CylCoords
import org.joml.{Matrix4f, Vector3d}

object Entity {
  def fromNbt(tag: Nbt.MapTag, registry: EntityRegistry)(using CylinderSize): Result[Entity, String] =
    val entType = tag.getString("type", "")
    registry.get(entType) match
      case Some(factory) => Ok(factory.fromNBT(tag))
      case None          => Err(s"Entity-type '$entType' not found")
}

class Entity(protected val data: EntityBaseData, val model: EntityModel) {
  def position: CylCoords = data.position
  def rotation: Vector3d = data.rotation
  def velocity: Vector3d = data.velocity

  def boundingBox: HexBox = new HexBox(0.5f, 0, 0.5f)

  def transform: Matrix4f = data.transform

  def tick(world: BlocksInWorld, collisionDetector: CollisionDetector): Unit = ()

  def toNBT: Nbt.MapTag =
    val dataNbt = data.toNBT

    Nbt.makeMap(
      "pos" -> dataNbt.pos,
      "velocity" -> dataNbt.velocity,
      "rotation" -> dataNbt.rotation
    )
}

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
