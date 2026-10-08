package hexacraft.client.entity

import hexacraft.world.CylinderSize
import hexacraft.world.entity.{EntityModel, EntityPart, HexPrism}

import org.joml.{Vector3d, Vector3dc, Vector3f}

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

  val model: EntityModel = create()

  private def create(): EntityModel = {
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

    val headPrism = HexPrism(headRadius, headDepth)
    val bodyPrism = HexPrism(bodyRadius, bodyLength)
    val legPrism = HexPrism(legRadius, legLength)

    val headPrismOffset = -headDepth / 2f

    val headDistX = 0.5 * bodyLength + headOffset
    val bodyDistX = 0.5 * bodyLength
    val legDistX = 0.5 * bodyLength - legRadius

    val headY = legLength + legYOffset + (headRadius + headYOffset) * CylinderSize.y60
    val bodyY = legLength + legYOffset
    val legY = legLength

    val headPos = pos(headDistX, headY, 0)
    val bodyPos = pos(-bodyDistX, bodyY, 0)
    val frontRightLegPos = pos(legDistX, legY, legSideOffset)
    val frontLeftLegPos = pos(legDistX, legY, -legSideOffset)
    val backRightLegPos = pos(-legDistX, legY, legSideOffset)
    val backLeftLegPos = pos(-legDistX, legY, -legSideOffset)

    val pi = math.Pi.toFloat

    EntityModel(
      IndexedSeq(
        EntityPart("head", headPrism, headPos, Vector3f(0, pi / 2, pi / 2), prismOffset = headPrismOffset),
        EntityPart("body", bodyPrism, bodyPos, Vector3f(0, pi / 2, -pi / 2)),
        EntityPart("frontRightLeg", legPrism, frontRightLegPos, Vector3f(pi, 0, 0)),
        EntityPart("frontLeftLeg", legPrism, frontLeftLegPos, Vector3f(pi, 0, 0)),
        EntityPart("backRightLeg", legPrism, backRightLegPos, Vector3f(pi, 0, 0)),
        EntityPart("backLeftLeg", legPrism, backLeftLegPos, Vector3f(pi, 0, 0))
      )
    )
  }
}
