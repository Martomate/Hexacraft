package hexacraft.world.entity

import hexacraft.world.{CylinderSize, HexBox}
import hexacraft.world.coord.CylCoords

import com.martomate.nbt.Nbt
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
  def fromNBT(tag: Nbt.MapTag)(using CylinderSize): TransformComponent = {
    val pos = tag
      .getMap("pos")
      .map(t => CylCoords(t.setVector(new Vector3d)))
      .getOrElse(CylCoords(0, 0, 0))

    val rot = new Vector3d
    tag.getMap("rotation").foreach(_.setVector(rot))

    TransformComponent(pos, rot)
  }
}

class VelocityComponent(var velocity: Vector3d = new Vector3d) extends EntityComponent

object VelocityComponent {
  def fromNBT(tag: Nbt.MapTag)(using CylinderSize): VelocityComponent = {
    val vel = new Vector3d
    tag.getMap("velocity").foreach(_.setVector(vel))

    VelocityComponent(vel)
  }
}

class ModelComponent(val model: EntityModel) extends EntityComponent

class AiComponent(val ai: EntityAI) extends EntityComponent

object AiComponent {
  def fromNBT(tag: Nbt.MapTag)(using CylinderSize): AiComponent = {
    val ai: EntityAI =
      tag.getMap("ai") match {
        case Some(t) => SimpleWalkAI.fromNBT(t)
        case None    => SimpleWalkAI.create
      }
    AiComponent(ai)
  }
}

class BoundsComponent(val bounds: HexBox) extends EntityComponent
