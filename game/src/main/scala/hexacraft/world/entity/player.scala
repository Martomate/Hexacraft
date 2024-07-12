package hexacraft.world.entity

import hexacraft.world.{CylinderSize, HexBox}
import hexacraft.world.coord.BlockCoords

import org.joml.Vector3f

class PlayerEntityModel(
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

  override def tick(walking: Boolean): Unit = {
    animation.tick(walking)
  }
}

class PlayerAnimation(model: PlayerEntityModel) {
  private var time = 0

  def tick(walking: Boolean): Unit = {
    if walking || time % 30 != 0 then {
      time += 1
    }

    val phase = time * (1f / 60) * 2 * math.Pi

    model.rightArm.rotation.z = -0.5f * math.sin(phase).toFloat
    model.leftArm.rotation.z = 0.5f * math.sin(phase).toFloat

    model.rightLeg.rotation.z = 0.5f * math.sin(phase).toFloat
    model.leftLeg.rotation.z = -0.5f * math.sin(phase).toFloat
  }
}

object PlayerEntityModel {
  private def makeHexBox(r: Int, b: Float, h: Int): HexBox = {
    HexBox(r / 32f * 0.5f, b / 32f * 0.5f, (h + b) / 32f * 0.5f)
  }

  private def makePartPosition(xp: Double, yp: Double, zp: Double): BlockCoords.Offset = {
    BlockCoords.Offset(xp / 32.0, yp / 32.0, zp / 32.0)
  }

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
    val armBounds = makeHexBox(armRadius, -armRadius * CylinderSize.y60.toFloat, armLength)
    val legBounds = makeHexBox(legRadius, 0, legLength)

    val headY = bodyLength + legLength + headRadius * CylinderSize.y60
    val headPos = makePartPosition(0, headY, 0).toCylCoordsOffset

    val rightBodyPos = makePartPosition(0, legLength, 0.5 * bodyRadius).toCylCoordsOffset
    val leftBodyPos = makePartPosition(0, legLength, -0.5 * bodyRadius).toCylCoordsOffset

    val armY = legLength + bodyLength - armRadius * CylinderSize.y60
    val rightArmPos = makePartPosition(0, armY, bodyRadius + 0.5 * armRadius).toCylCoordsOffset
    val leftArmPos = makePartPosition(0, armY, -bodyRadius - 0.5 * armRadius).toCylCoordsOffset

    val rightLegPos = makePartPosition(0, legLength, 0.5 * legRadius).offset(0, 0, 0.001f).toCylCoordsOffset
    val leftLegPos = makePartPosition(0, legLength, -0.5 * legRadius).offset(0, 0, -0.001).toCylCoordsOffset

    val pi = math.Pi.toFloat

    PlayerEntityModel(
      head = BasicEntityPart(headBounds, headPos, Vector3f(0, pi / 2, pi / 2), (0, 176)),
      leftBodyHalf = BasicEntityPart(bodyBounds, leftBodyPos, Vector3f(0, 0, 0), (0, 120)),
      rightBodyHalf = BasicEntityPart(bodyBounds, rightBodyPos, Vector3f(0, 0, 0), (48, 120)),
      rightArm = BasicEntityPart(armBounds, rightArmPos, Vector3f(pi, 0, 0), (48, 64)),
      leftArm = BasicEntityPart(armBounds, leftArmPos, Vector3f(pi, 0, 0), (0, 64)),
      rightLeg = BasicEntityPart(legBounds, rightLegPos, Vector3f(pi, 0, 0), (48, 0)),
      leftLeg = BasicEntityPart(legBounds, leftLegPos, Vector3f(pi, 0, 0), (0, 0)),
      textureName
    )
  }
}
