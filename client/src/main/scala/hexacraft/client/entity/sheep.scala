package hexacraft.client.entity

import hexacraft.world.CylinderSize
import hexacraft.world.entity.{BasicEntityPart, EntityModel, EntityPart}

import org.joml.{Vector3d, Vector3dc, Vector3f}

class SheepEntityModel(
    val head: BasicEntityPart,
    val body: BasicEntityPart,
    val frontRightLeg: BasicEntityPart,
    val frontLeftLeg: BasicEntityPart,
    val backRightLeg: BasicEntityPart,
    val backLeftLeg: BasicEntityPart,
    val textureName: String
) extends EntityModel {
  override val parts: Seq[EntityPart] = Seq(head, body, frontRightLeg, frontLeftLeg, backRightLeg, backLeftLeg)

  private val animation = new SheepAnimation(this)

  override def tick(
      walking: Boolean,
      headDirection: Option[Vector3d],
      rotation: Vector3dc,
      mountRotation: Option[Vector3dc]
  ): Unit = {
    animation.tick(walking)
  }
}

class SheepAnimation(model: SheepEntityModel) {
  var time = 0f

  def tick(walking: Boolean): Unit = {
    if walking || time % 30 != 0 then {
      time += 1
    }

    val phase = time * (1f / 60) * 2 * math.Pi

    model.frontRightLeg.rotation.z = -0.5f * math.sin(phase).toFloat
    model.frontLeftLeg.rotation.z = 0.5f * math.sin(phase).toFloat

    model.backRightLeg.rotation.z = 0.5f * math.sin(phase).toFloat
    model.backLeftLeg.rotation.z = -0.5f * math.sin(phase).toFloat
  }
}

object SheepEntityModel {
  import ModelUnits.*

  def create(textureName: String): SheepEntityModel = {
    val legLength = 32
    val legRadius = 6
    val bodyLength = 48
    val bodyRadius = 16
    val headDepth = 16
    val headRadius = 12

    val headOffset = 2
    val headYOffset = 2
    val legSideOffset = 0.5 * bodyRadius
    val legYOffset = 3

    val headBounds = makeHexBox(headRadius, -headDepth / 2f, headDepth)
    val bodyBounds = makeHexBox(bodyRadius, 0, bodyLength)
    val legBounds = makeHexBox(legRadius, 0, legLength)

    val headDistX = 0.5 * bodyLength + headOffset
    val bodyDistX = 0.5 * bodyLength
    val legDistX = 0.5 * bodyLength - legRadius

    val headY = legLength + legYOffset + (headRadius + headYOffset) * CylinderSize.y60
    val bodyY = legLength + legYOffset
    val legY = legLength

    val headPos = cylOffset(headDistX, headY, 0)
    val bodyPos = cylOffset(-bodyDistX, bodyY, 0)
    val frontRightLegPos = cylOffset(legDistX, legY, legSideOffset)
    val frontLeftLegPos = cylOffset(legDistX, legY, -legSideOffset)
    val backRightLegPos = cylOffset(-legDistX, legY, legSideOffset)
    val backLeftLegPos = cylOffset(-legDistX, legY, -legSideOffset)

    val pi = math.Pi.toFloat

    new SheepEntityModel(
      head = BasicEntityPart(headBounds, headPos, Vector3f(0, pi / 2, pi / 2), (0, 168)),
      body = BasicEntityPart(bodyBounds, bodyPos, Vector3f(0, pi / 2, -pi / 2), (0, 88)),
      frontRightLeg = BasicEntityPart(legBounds, frontRightLegPos, Vector3f(pi, 0, 0), (36, 44)),
      frontLeftLeg = BasicEntityPart(legBounds, frontLeftLegPos, Vector3f(pi, 0, 0), (0, 44)),
      backRightLeg = BasicEntityPart(legBounds, backRightLegPos, Vector3f(pi, 0, 0), (36, 0)),
      backLeftLeg = BasicEntityPart(legBounds, backLeftLegPos, Vector3f(pi, 0, 0), (0, 0)),
      textureName
    )
  }
}
