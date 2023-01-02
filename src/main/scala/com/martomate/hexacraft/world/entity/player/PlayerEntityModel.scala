package com.martomate.hexacraft.world.entity.player

import com.martomate.hexacraft.renderer.TextureSingle
import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.HexBox
import com.martomate.hexacraft.world.coord.fp.BlockCoords
import com.martomate.hexacraft.world.entity.{EntityModel, EntityPart}
import com.martomate.hexacraft.world.entity.base.BasicEntityPart

import com.eclipsesource.json.JsonObject
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

  override def texSize: Int = 256

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
  def fromJson(setup: JsonObject): PlayerEntityModel =
    val partsNBT = setup.get("parts").asObject()

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
      new Vector3f(0, math.Pi.toFloat / 2, math.Pi.toFloat / 2),
      setup = partsNBT.get("head").asObject()
    )
    val leftBodyHalf = new BasicEntityPart(
      makeHexBox(bodyRadius, 0, bodyLength),
      BlockCoords.Offset(0, legLength / 32f, -0.5f * bodyRadius / 32).toCylCoordsOffset,
      new Vector3f(0, 0, 0),
      setup = partsNBT.get("leftbody").asObject()
    )
    val rightBodyHalf = new BasicEntityPart(
      makeHexBox(bodyRadius, 0, bodyLength),
      BlockCoords.Offset(0, legLength / 32f, 0.5f * bodyRadius / 32).toCylCoordsOffset,
      new Vector3f(0, 0, 0),
      setup = partsNBT.get("rightbody").asObject()
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
      new Vector3f(math.Pi.toFloat * -6 / 6, 0, 0),
      setup = partsNBT.get("rightarm").asObject()
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
      new Vector3f(math.Pi.toFloat * 6 / 6, 0, 0),
      setup = partsNBT.get("leftarm").asObject()
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
      new Vector3f(math.Pi.toFloat, 0, 0),
      setup = partsNBT.get("rightleg").asObject()
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
      new Vector3f(math.Pi.toFloat, 0, 0),
      setup = partsNBT.get("leftleg").asObject()
    )

    new PlayerEntityModel(
      head,
      leftBodyHalf,
      rightBodyHalf,
      rightArm,
      leftArm,
      rightLeg,
      leftLeg,
      setup.getString("texture", "")
    )
