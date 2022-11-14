package com.martomate.hexacraft.world.entity.player

import com.eclipsesource.json.JsonObject
import com.martomate.hexacraft.renderer.TextureSingle
import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.HexBox
import com.martomate.hexacraft.world.coord.fp.BlockCoords
import com.martomate.hexacraft.world.entity.base.BasicEntityPart
import com.martomate.hexacraft.world.entity.{EntityModel, EntityPart}
import org.joml.Vector3f

class PlayerEntityModel(setup: JsonObject)(implicit cylinderSize: CylinderSize) extends EntityModel {
  private val partsNBT = setup.get("parts").asObject()

  def makeHexBox(r: Int, b: Float, h: Int): HexBox =
    new HexBox(r / 32f * 0.5f, b / 32f * 0.5f, (h + b) / 32f * 0.5f)

  private val legLength = 48
  private val legRadius = 8
  private val bodyLength = 40
  private val bodyRadius = 8
  private val armLength = 40
  private val armRadius = 8
  private val headDepth = 20
  private val headRadius = 16

  private val head = new BasicEntityPart(
    makeHexBox(headRadius, -headDepth / 2f, headDepth),
//    BlockCoords(0, (bodyLength + legLength) / 32f + headDepth / 2 / 32f, 0).toCylCoords,
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
  private val leftBodyhalf = new BasicEntityPart(
    makeHexBox(bodyRadius, 0, bodyLength),
    BlockCoords.Offset(0, legLength / 32f, -0.5f * bodyRadius / 32).toCylCoordsOffset,
    new Vector3f(0, 0, 0),
    setup = partsNBT.get("leftbody").asObject()
  )
  private val rightBodyhalf = new BasicEntityPart(
    makeHexBox(bodyRadius, 0, bodyLength),
    BlockCoords.Offset(0, legLength / 32f, 0.5f * bodyRadius / 32).toCylCoordsOffset,
    new Vector3f(0, 0, 0),
    setup = partsNBT.get("rightbody").asObject()
  )
  private val rightArm = new BasicEntityPart(
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
  private val leftArm = new BasicEntityPart(
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
  private val rightLeg = new BasicEntityPart(
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
  private val leftLeg = new BasicEntityPart(
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

  override val parts: Seq[EntityPart] =
    Seq(head, leftBodyhalf, rightBodyhalf, rightArm, leftArm, rightLeg, leftLeg)
  var time = 0f
  override def tick(): Unit = {
    time += 1f / 60

    val phase = time * 2 * math.Pi

    rightArm.rotation.z = -0.5f * math.sin(phase).toFloat
    leftArm.rotation.z = 0.5f * math.sin(phase).toFloat

//    rightArm.rotation.x += -0.03f * math.sin(time).toFloat
//    leftArm.rotation.x +=  0.03f * math.sin(time).toFloat

    rightLeg.rotation.z = 0.5f * math.sin(phase).toFloat
    leftLeg.rotation.z = -0.5f * math.sin(phase).toFloat
  }

  override def texture: TextureSingle =
    TextureSingle.getTexture(
      "textures/entities/" + setup.getString("texture", "")
    )

  override def texSize: Int = 256
}
