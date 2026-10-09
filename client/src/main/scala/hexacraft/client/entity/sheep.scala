package hexacraft.client.entity

import org.joml.{Vector3d, Vector3dc}

class SheepAnimation(pose: EntityPose) extends EntityAnimation {
  private val frontRightLeg = pose.rotation("frontRightLeg")
  private val frontLeftLeg = pose.rotation("frontLeftLeg")
  private val backRightLeg = pose.rotation("backRightLeg")
  private val backLeftLeg = pose.rotation("backLeftLeg")

  private var time = 0f

  override def tick(
      walking: Boolean,
      headDirection: Option[Vector3d],
      rotation: Vector3dc,
      mountRotation: Option[Vector3dc]
  ): Unit = {
    if walking || time % 30 != 0 then {
      time += 1
    }

    val phase = time * (1f / 60) * 2 * math.Pi

    frontRightLeg.z = -0.5f * math.sin(phase).toFloat
    frontLeftLeg.z = 0.5f * math.sin(phase).toFloat

    backRightLeg.z = 0.5f * math.sin(phase).toFloat
    backLeftLeg.z = -0.5f * math.sin(phase).toFloat
  }
}
