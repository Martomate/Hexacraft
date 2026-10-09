package hexacraft.client.entity

import hexacraft.game.PlayerInputHandler

import org.joml.{Vector3d, Vector3dc}

class PlayerAnimation(pose: EntityPose) extends EntityAnimation {
  private val base = pose.rotation("base")
  private val headYawBase = pose.rotation("headYawBase")
  private val headBase = pose.rotation("headBase")
  private val rightArm = pose.rotation("rightArm")
  private val leftArm = pose.rotation("leftArm")
  private val rightLeg = pose.rotation("rightLeg")
  private val leftLeg = pose.rotation("leftLeg")

  private var time = 0

  override def tick(
      walking: Boolean,
      headDirection: Option[Vector3d],
      rotation: Vector3dc,
      mountRotation: Option[Vector3dc]
  ): Unit = {
    val sitting = mountRotation.isDefined

    if walking || time % 30 != 0 then {
      time += 1
    }

    val phase = time * (1f / 60) * 2 * math.Pi

    rightArm.z = -0.5f * math.sin(phase).toFloat
    leftArm.z = 0.5f * math.sin(phase).toFloat

    if sitting then {
      rightLeg.z = math.Pi.toFloat * 0.5f
      leftLeg.z = math.Pi.toFloat * 0.5f
    } else {
      rightLeg.z = 0.5f * math.sin(phase).toFloat
      leftLeg.z = -0.5f * math.sin(phase).toFloat
    }

    // While sitting, the body faces the same way as the mount and only the head follows the look direction
    val headYaw = mountRotation match {
      case Some(mountRot) =>
        val maxYaw = PlayerInputHandler.MaxHeadYawWhenMounted
        math.max(-maxYaw, math.min(maxYaw, PlayerInputHandler.wrapAngle(rotation.y - mountRot.y)))
      case None => 0.0
    }
    // The body is turned back so that only the head turns
    base.y = -headYaw.toFloat
    headYawBase.y = headYaw.toFloat

    headBase.z = -headDirection.map(_.x).getOrElse(0.0).toFloat
  }
}
