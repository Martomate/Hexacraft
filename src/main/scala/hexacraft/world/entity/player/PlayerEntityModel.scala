package hexacraft.world.entity.player

import hexacraft.renderer.TextureSingle
import hexacraft.world.CylinderSize
import hexacraft.world.block.HexBox
import hexacraft.world.coord.fp.BlockCoords
import hexacraft.world.entity.{EntityModel, EntityPart}
import hexacraft.world.entity.base.BasicEntityPart

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
) extends EntityModel:
  override val parts: Seq[EntityPart] = Seq(head, leftBodyHalf, rightBodyHalf, rightArm, leftArm, rightLeg, leftLeg)

  private val animation = new PlayerAnimation(this)

  override def tick(): Unit = animation.tick()

  override def texture: TextureSingle =
    TextureSingle.getTexture(
      "textures/entities/" + textureName
    )

class PlayerAnimation(model: PlayerEntityModel):
  private var time = 0f

  def tick(): Unit =
    time += 1f / 60

    val phase = time * 2 * math.Pi

    model.rightArm.rotation.z = -0.5f * math.sin(phase).toFloat
    model.leftArm.rotation.z = 0.5f * math.sin(phase).toFloat

    model.rightLeg.rotation.z = 0.5f * math.sin(phase).toFloat
    model.leftLeg.rotation.z = -0.5f * math.sin(phase).toFloat

object PlayerEntityModel:
  def create(textureName: String): PlayerEntityModel =
    def makeHexBox(r: Int, b: Float, h: Int): HexBox =
      new HexBox(r / 32f * 0.5f, b / 32f * 0.5f, (h + b) / 32f * 0.5f)

    val legLength = 48
    val legRadius = 8
    val bodyLength = 40
    val bodyRadius = 8
    val armLength = 40
    val armRadius = 8
    val headDepth = 20
    val headRadius = 16

    val head = new BasicEntityPart(
      makeHexBox(headRadius, -headDepth / 2f, headDepth),
      BlockCoords
        .Offset(
          0,
          (bodyLength + legLength) / 32f + headRadius / 32f * CylinderSize.y60,
          0
        )
        .toCylCoordsOffset,
      Vector3f(0, math.Pi.toFloat / 2, math.Pi.toFloat / 2),
      (0, 176)
    )
    val leftBodyHalf = new BasicEntityPart(
      makeHexBox(bodyRadius, 0, bodyLength),
      BlockCoords.Offset(0, legLength / 32f, -0.5f * bodyRadius / 32).toCylCoordsOffset,
      Vector3f(0, 0, 0),
      (0, 120)
    )
    val rightBodyHalf = new BasicEntityPart(
      makeHexBox(bodyRadius, 0, bodyLength),
      BlockCoords.Offset(0, legLength / 32f, 0.5f * bodyRadius / 32).toCylCoordsOffset,
      Vector3f(0, 0, 0),
      (48, 120)
    )
    val rightArm = new BasicEntityPart(
      makeHexBox(armRadius, -armRadius * CylinderSize.y60.toFloat, armLength),
      BlockCoords
        .Offset(
          0,
          (legLength + bodyLength - armRadius * CylinderSize.y60.toFloat) / 32f,
          0.5f * 2 * bodyRadius / 32 + 0.5f * armRadius / 32
        )
        .toCylCoordsOffset,
      Vector3f(math.Pi.toFloat * -6 / 6, 0, 0),
      (48, 64)
    )
    val leftArm = new BasicEntityPart(
      makeHexBox(armRadius, -armRadius * CylinderSize.y60.toFloat, armLength),
      BlockCoords
        .Offset(
          0,
          (legLength + bodyLength - armRadius * CylinderSize.y60.toFloat) / 32f,
          -0.5f * 2 * bodyRadius / 32 - 0.5f * armRadius / 32
        )
        .toCylCoordsOffset,
      Vector3f(math.Pi.toFloat * 6 / 6, 0, 0),
      (0, 64)
    )
    val rightLeg = new BasicEntityPart(
      makeHexBox(legRadius, 0, legLength),
      BlockCoords
        .Offset(
          0,
          legLength / 32f,
          0.5f * legRadius / 32 + 0.001f
        )
        .toCylCoordsOffset,
      Vector3f(math.Pi.toFloat, 0, 0),
      (48, 0)
    )
    val leftLeg = new BasicEntityPart(
      makeHexBox(legRadius, 0, legLength),
      BlockCoords
        .Offset(
          0,
          legLength / 32f,
          -0.5f * legRadius / 32 - 0.001f
        )
        .toCylCoordsOffset,
      Vector3f(math.Pi.toFloat, 0, 0),
      (0, 0)
    )

    new PlayerEntityModel(
      head,
      leftBodyHalf,
      rightBodyHalf,
      rightArm,
      leftArm,
      rightLeg,
      leftLeg,
      textureName
    )
