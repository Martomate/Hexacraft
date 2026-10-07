package hexacraft.client.entity

import hexacraft.world.{CylinderSize, HexBox}
import hexacraft.world.entity.{BasicEntityPart, EntityModel, EntityPart}

import org.joml.{Vector3d, Vector3f}

class PlayerEntityModel(
    val headBase: BasicEntityPart, // not rendered
    val head: BasicEntityPart,
    val leftBodyHalf: BasicEntityPart,
    val rightBodyHalf: BasicEntityPart,
    val rightArm: BasicEntityPart,
    val leftArm: BasicEntityPart,
    val rightLeg: BasicEntityPart,
    val leftLeg: BasicEntityPart,
    val textureName: String
) extends EntityModel {
  override val parts: Seq[EntityPart] = Seq(head, leftBodyHalf, rightBodyHalf, rightArm, leftArm, rightLeg, leftLeg)

  private val animation = new PlayerAnimation(this)

  override def tick(walking: Boolean, headDirection: Option[Vector3d], sitting: Boolean): Unit = {
    animation.tick(walking, headDirection.getOrElse(new Vector3d), sitting)
  }
}

class PlayerAnimation(model: PlayerEntityModel) {
  private var time = 0

  def tick(walking: Boolean, headDirection: Vector3d, sitting: Boolean): Unit = {
    println(sitting)

    if walking || time % 30 != 0 then {
      time += 1
    }

    val phase = time * (1f / 60) * 2 * math.Pi

    model.rightArm.rotation.z = -0.5f * math.sin(phase).toFloat
    model.leftArm.rotation.z = 0.5f * math.sin(phase).toFloat

    if sitting then {
      model.rightLeg.rotation.z = math.Pi.toFloat * 0.5f
      model.leftLeg.rotation.z = math.Pi.toFloat * 0.5f
    } else {
      model.rightLeg.rotation.z = 0.5f * math.sin(phase).toFloat
      model.leftLeg.rotation.z = -0.5f * math.sin(phase).toFloat
    }

    model.headBase.rotation.z = -headDirection.x.toFloat
  }
}

object PlayerEntityModel {
  import ModelUnits.*

  def create(textureName: String): PlayerEntityModel = {
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

    val base = BasicEntityPart(HexBox(0, 0, 0), cylOffset(0, 0, 0), Vector3f(0, pi / 2, 0))

    val headBase = BasicEntityPart(HexBox(0, 0, 0), headBasePos, Vector3f(), parentPart = base)
    val head = BasicEntityPart(headBounds, headPos, Vector3f(0, pi / 2, pi / 2), (0, 176), parentPart = headBase)

    PlayerEntityModel(
      headBase = headBase,
      head = head,
      leftBodyHalf = BasicEntityPart(bodyBounds, leftBodyPos, Vector3f(0, 0, 0), (0, 120), parentPart = base),
      rightBodyHalf = BasicEntityPart(bodyBounds, rightBodyPos, Vector3f(0, 0, 0), (48, 120), parentPart = base),
      rightArm = BasicEntityPart(armBounds, rightArmPos, Vector3f(pi, 0, 0), (48, 64), parentPart = base),
      leftArm = BasicEntityPart(armBounds, leftArmPos, Vector3f(pi, 0, 0), (0, 64), parentPart = base),
      rightLeg = BasicEntityPart(legBounds, rightLegPos, Vector3f(pi, 0, 0), (48, 0), parentPart = base),
      leftLeg = BasicEntityPart(legBounds, leftLegPos, Vector3f(pi, 0, 0), (0, 0), parentPart = base),
      textureName
    )
  }
}
