package hexacraft.client.entity

import hexacraft.world.CylinderSize
import hexacraft.world.entity.{EntityModel, EntityPart}

import org.joml.{Vector3d, Vector3dc, Vector3f}

class SheepAnimation(model: EntityModel) extends EntityAnimation {
  private val frontRightLeg = model.part("frontRightLeg")
  private val frontLeftLeg = model.part("frontLeftLeg")
  private val backRightLeg = model.part("backRightLeg")
  private val backLeftLeg = model.part("backLeftLeg")

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

    frontRightLeg.rotation.z = -0.5f * math.sin(phase).toFloat
    frontLeftLeg.rotation.z = 0.5f * math.sin(phase).toFloat

    backRightLeg.rotation.z = 0.5f * math.sin(phase).toFloat
    backLeftLeg.rotation.z = -0.5f * math.sin(phase).toFloat
  }
}

object SheepEntityModel {
  import ModelUnits.*

  val skin: EntitySkin = EntitySkin(
    "sheep",
    Map(
      "head" -> (0, 168),
      "body" -> (0, 88),
      "frontRightLeg" -> (36, 44),
      "frontLeftLeg" -> (0, 44),
      "backRightLeg" -> (36, 0),
      "backLeftLeg" -> (0, 0)
    )
  )

  def create(): EntityModel = {
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

    EntityModel(
      Seq(
        EntityPart("head", headBounds, headPos, Vector3f(0, pi / 2, pi / 2)),
        EntityPart("body", bodyBounds, bodyPos, Vector3f(0, pi / 2, -pi / 2)),
        EntityPart("frontRightLeg", legBounds, frontRightLegPos, Vector3f(pi, 0, 0)),
        EntityPart("frontLeftLeg", legBounds, frontLeftLegPos, Vector3f(pi, 0, 0)),
        EntityPart("backRightLeg", legBounds, backRightLegPos, Vector3f(pi, 0, 0)),
        EntityPart("backLeftLeg", legBounds, backLeftLegPos, Vector3f(pi, 0, 0))
      )
    )
  }
}
