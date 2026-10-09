package hexacraft.server.entity

import hexacraft.world.CylinderSize
import hexacraft.world.entity.{EntityModel, EntityPart, HexPrism}

import org.joml.Vector3f

object PlayerEntityModel {
  import ModelUnits.*

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
