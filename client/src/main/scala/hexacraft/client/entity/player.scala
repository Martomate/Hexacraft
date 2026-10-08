package hexacraft.client.entity

import hexacraft.game.PlayerInputHandler
import hexacraft.world.CylinderSize
import hexacraft.world.entity.{EntityModel, EntityPart, HexPrism}

import org.joml.{Vector3d, Vector3dc, Vector3f}

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

  val model: EntityModel = create()

  private def create(): EntityModel = {
    val legLength = 48
    val legRadius = 8
    val bodyLength = 40
    val bodyRadius = 8
    val armLength = 40
    val armRadius = 8
    val headDepth = 20
    val headRadius = 16

    val headPrism = HexPrism(headRadius, headDepth)
    val bodyPrism = HexPrism(bodyRadius, bodyLength)
    val armPrism = HexPrism(armRadius, armLength)
    val legPrism = HexPrism(legRadius, legLength)

    val headPrismOffset = -headDepth / 2f
    val armPrismOffset = (-armRadius * CylinderSize.y60).toFloat

    val headBaseY = bodyLength + legLength
    val headY = headRadius * CylinderSize.y60
    val headBasePos = pos(0, headBaseY, 0)
    val headPos = pos(0, headY, 0)

    // The body halves are neighbouring hexagons, so are the arms and the body
    val rightBodyPos = pos(0, legLength, 0.5 * hexStep(bodyRadius))
    val leftBodyPos = pos(0, legLength, -0.5 * hexStep(bodyRadius))

    val armY = legLength + bodyLength - armRadius * CylinderSize.y60
    val rightArmPos = pos(0, armY, hexStep(bodyRadius + 0.5 * armRadius))
    val leftArmPos = pos(0, armY, -hexStep(bodyRadius + 0.5 * armRadius))

    // The legs are moved a tiny bit apart to avoid z-fighting
    val legGap = 0.05
    val rightLegPos = pos(0, legLength, 0.5 * hexStep(legRadius) + legGap)
    val leftLegPos = pos(0, legLength, -0.5 * hexStep(legRadius) - legGap)

    val pi = math.Pi.toFloat

    // Pivots (not rendered)
    val base = EntityPart("base", HexPrism.empty, pos(0, 0, 0), Vector3f(0, pi / 2, 0))
    val headYawBase = EntityPart("headYawBase", HexPrism.empty, headBasePos, Vector3f(), Some("base"))
    val headBase = EntityPart("headBase", HexPrism.empty, pos(0, 0, 0), Vector3f(), Some("headYawBase"))

    EntityModel(
      IndexedSeq(
        base,
        headYawBase,
        headBase,
        EntityPart("head", headPrism, headPos, Vector3f(0, pi / 2, pi / 2), Some("headBase"), headPrismOffset),
        EntityPart("leftBodyHalf", bodyPrism, leftBodyPos, Vector3f(0, 0, 0), Some("base")),
        EntityPart("rightBodyHalf", bodyPrism, rightBodyPos, Vector3f(0, 0, 0), Some("base")),
        EntityPart("rightArm", armPrism, rightArmPos, Vector3f(pi, 0, 0), Some("base"), armPrismOffset),
        EntityPart("leftArm", armPrism, leftArmPos, Vector3f(pi, 0, 0), Some("base"), armPrismOffset),
        EntityPart("rightLeg", legPrism, rightLegPos, Vector3f(pi, 0, 0), Some("base")),
        EntityPart("leftLeg", legPrism, leftLegPos, Vector3f(pi, 0, 0), Some("base"))
      )
    )
  }
}
