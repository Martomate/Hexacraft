package hexacraft.world.entity

import hexacraft.nbt.{Nbt, NbtDecoder}
import hexacraft.world.{CylinderSize, HexBox}
import hexacraft.world.coord.CylCoords

import org.joml.{Matrix4f, Vector3d}

trait EntityComponent

class TransformComponent(var position: CylCoords, var rotation: Vector3d = new Vector3d) extends EntityComponent {
  def transform: Matrix4f = {
    new Matrix4f()
      .translate(position.toVector3f)
      .rotateZ(rotation.z.toFloat)
      .rotateX(rotation.x.toFloat)
      .rotateY(rotation.y.toFloat)
  }
}

object TransformComponent {
  given (using CylinderSize): NbtDecoder[TransformComponent] with {
    override def decode(tag: Nbt.MapTag): Option[TransformComponent] = {
      val pos = tag
        .getMap("pos")
        .map(t => CylCoords(t.setVector(new Vector3d)))
        .getOrElse(CylCoords(0, 0, 0))

      val rot = new Vector3d
      tag.getMap("rotation").foreach(_.setVector(rot))

      Some(TransformComponent(pos, rot))
    }
  }
}

class MotionComponent(
    var velocity: Vector3d = new Vector3d,
    var flying: Boolean = false
) extends EntityComponent

object MotionComponent {
  given (using CylinderSize): NbtDecoder[MotionComponent] with {
    override def decode(tag: Nbt.MapTag): Option[MotionComponent] = {
      val vel = new Vector3d
      tag.getMap("velocity").foreach(_.setVector(vel))

      val flying = tag.getBoolean("flying", false)

      Some(MotionComponent(vel, flying))
    }
  }
}

@deprecated("a better solution needs to be made soon")
class HeadDirectionComponent(var direction: Vector3d = new Vector3d) extends EntityComponent

object HeadDirectionComponent {
  given (using CylinderSize): NbtDecoder[HeadDirectionComponent] with {
    override def decode(tag: Nbt.MapTag): Option[HeadDirectionComponent] = {
      val dir = new Vector3d
      tag.getMap("head_direction").foreach(_.setVector(dir))

      Some(HeadDirectionComponent(dir))
    }
  }
}

class ModelComponent(val model: EntityModel) extends EntityComponent

class AiComponent(val ai: EntityAI) extends EntityComponent

object AiComponent {
  given (using CylinderSize): NbtDecoder[AiComponent] with {
    override def decode(tag: Nbt.MapTag): Option[AiComponent] = {
      val ai: EntityAI = tag.getMap("ai") match {
        case Some(t) => Nbt.decode[SimpleWalkAI](t).get
        case None    => SimpleWalkAI.create
      }
      Some(AiComponent(ai))
    }
  }
}

class BoundsComponent(val bounds: HexBox) extends EntityComponent
