package hexacraft.world.entity

import com.flowpowered.nbt.Tag
import hexacraft.nbt.{Nbt, NBTUtil}
import hexacraft.util.Result
import hexacraft.util.Result.{Err, Ok}
import hexacraft.world.{BlocksInWorld, CollisionDetector, CylinderSize}
import hexacraft.world.block.{Blocks, HexBox}
import hexacraft.world.coord.fp.CylCoords
import org.joml.{Matrix4f, Vector3d}

object Entity {
  def fromNbt(tag: Nbt.MapTag, registry: EntityRegistry)(using CylinderSize, Blocks): Result[Entity, String] =
    val entType = tag.getString("type", "")
    registry.get(entType) match
      case Some(factory) => Ok(factory.fromNBT(tag.toCompoundTag("")))
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

// TODO: Create an Entity.Snapshot class that contains all the data needed for an entity, and make sure that an Entity
//  can be converted to and from the Snapshot type. The Snapshot can then be converted to and from NBT.
