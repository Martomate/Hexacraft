package hexacraft.client.entity

import hexacraft.game.PlayerInputHandler
import hexacraft.world.{CylinderSize, HexBox}
import hexacraft.world.entity.{EntityModel, EntityPart}

import org.joml.{Vector3d, Vector3dc, Vector3f}

class PlayerAnimation(model: EntityModel) extends EntityAnimation {
  private val base = model.part("base")
  private val headYawBase = model.part("headYawBase")
  private val headBase = model.part("headBase")
  private val rightArm = model.part("rightArm")
  private val leftArm = model.part("leftArm")
  private val rightLeg = model.part("rightLeg")
  private val leftLeg = model.part("leftLeg")

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

    rightArm.rotation.z = -0.5f * math.sin(phase).toFloat
    leftArm.rotation.z = 0.5f * math.sin(phase).toFloat

    if sitting then {
      rightLeg.rotation.z = math.Pi.toFloat * 0.5f
      leftLeg.rotation.z = math.Pi.toFloat * 0.5f
    } else {
      rightLeg.rotation.z = 0.5f * math.sin(phase).toFloat
      leftLeg.rotation.z = -0.5f * math.sin(phase).toFloat
    }

    // While sitting, the body faces the same way as the mount and only the head follows the look direction
    val headYaw = mountRotation match {
      case Some(mountRot) =>
        val maxYaw = PlayerInputHandler.MaxHeadYawWhenMounted
        math.max(-maxYaw, math.min(maxYaw, PlayerInputHandler.wrapAngle(rotation.y - mountRot.y)))
      case None => 0.0
    }
    base.rotation.y = (math.Pi / 2 - headYaw).toFloat
    headYawBase.rotation.y = headYaw.toFloat

    headBase.rotation.z = -headDirection.map(_.x).getOrElse(0.0).toFloat
  }
}

object PlayerEntityModel {
  import ModelUnits.*

  val skin: EntitySkin = EntitySkin(
    "player",
    Map(
      "head" -> (0, 176),
      "leftBodyHalf" -> (0, 120),
      "rightBodyHalf" -> (48, 120),
      "rightArm" -> (48, 64),
      "leftArm" -> (0, 64),
      "rightLeg" -> (48, 0),
      "leftLeg" -> (0, 0)
    )
  )

  def create(): EntityModel = {
    val legLength = 48
    val legRadius = 8
    val bodyLength = 40
    val bodyRadius = 8
    val armLength = 40
    val armRadius = 8
    val headDepth = 20
    val headRadius = 16

    val headBounds = makeHexBox(headRadius, -headDepth / 2f, headDepth)
    val bodyBounds = makeHexBox(bodyRadius, 0, bodyLength)
    val armBounds = makeHexBox(armRadius, -armRadius * CylinderSize.y60, armLength)
    val legBounds = makeHexBox(legRadius, 0, legLength)

    val headBaseY = bodyLength + legLength
    val headY = headRadius * CylinderSize.y60
    val headBasePos = cylOffset(0, headBaseY, 0)
    val headPos = cylOffset(0, headY, 0)

    // The body halves are neighbouring hexagons, so are the arms and the body
    val rightBodyPos = cylOffset(0, legLength, 0.5 * hexStep(bodyRadius))
    val leftBodyPos = cylOffset(0, legLength, -0.5 * hexStep(bodyRadius))

    val armY = legLength + bodyLength - armRadius * CylinderSize.y60
    val rightArmPos = cylOffset(0, armY, hexStep(bodyRadius + 0.5 * armRadius))
    val leftArmPos = cylOffset(0, armY, -hexStep(bodyRadius + 0.5 * armRadius))

    // The legs are moved a tiny bit apart to avoid z-fighting
    val legGap = 0.001
    val rightLegPos = cylOffset(0, legLength, 0.5 * hexStep(legRadius)).offset(0, 0, legGap)
    val leftLegPos = cylOffset(0, legLength, -0.5 * hexStep(legRadius)).offset(0, 0, -legGap)

    val pi = math.Pi.toFloat

    // Pivots (not rendered)
    val base = EntityPart("base", HexBox(0, 0, 0), cylOffset(0, 0, 0), Vector3f(0, pi / 2, 0))
    val headYawBase = EntityPart("headYawBase", HexBox(0, 0, 0), headBasePos, Vector3f(), parent = Some(base))
    val headBase = EntityPart("headBase", HexBox(0, 0, 0), cylOffset(0, 0, 0), Vector3f(), parent = Some(headYawBase))

    EntityModel(
      Seq(
        base,
        headYawBase,
        headBase,
        EntityPart("head", headBounds, headPos, Vector3f(0, pi / 2, pi / 2), Some(headBase)),
        EntityPart("leftBodyHalf", bodyBounds, leftBodyPos, Vector3f(0, 0, 0), Some(base)),
        EntityPart("rightBodyHalf", bodyBounds, rightBodyPos, Vector3f(0, 0, 0), Some(base)),
        EntityPart("rightArm", armBounds, rightArmPos, Vector3f(pi, 0, 0), Some(base)),
        EntityPart("leftArm", armBounds, leftArmPos, Vector3f(pi, 0, 0), Some(base)),
        EntityPart("rightLeg", legBounds, rightLegPos, Vector3f(pi, 0, 0), Some(base)),
        EntityPart("leftLeg", legBounds, leftLegPos, Vector3f(pi, 0, 0), Some(base))
      )
    )
  }
}
