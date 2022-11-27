package com.martomate.hexacraft.world.entity.sheep

import com.martomate.hexacraft.renderer.TextureSingle
import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.HexBox
import com.martomate.hexacraft.world.coord.fp.BlockCoords
import com.martomate.hexacraft.world.entity.{EntityModel, EntityPart}
import com.martomate.hexacraft.world.entity.base.BasicEntityPart

import com.eclipsesource.json.JsonObject
import org.joml.Vector3f

class SheepEntityModel(setup: JsonObject)(implicit cylinderSize: CylinderSize) extends EntityModel {
  private val partsNBT = setup.get("parts").asObject()

  def makeHexBox(r: Int, b: Float, h: Int): HexBox =
    new HexBox(r / 32f * 0.5f, b / 32f * 0.5f, (h + b) / 32f * 0.5f)

  private val legLength = 32
  private val legRadius = 6
  private val bodyLength = 48
  private val bodyRadius = 16
  private val headDepth = 16
  private val headRadius = 12

  private val headOffset = 2
  private val headYOffset = 2
  private val legOffset = bodyRadius * 0.25f / CylinderSize.y60
  private val legYOffset = 3

  private val pixSizeX: Double = 0.5 / CylinderSize.y60 / CylinderSize.y60 / 32
  private val pixSizeZ: Double = pixSizeX * 0.5

  private val totalXOffset = -bodyLength / 2

  private val head = new BasicEntityPart(
    makeHexBox(headRadius, -headDepth / 2f, headDepth),
    BlockCoords
      .Offset(
        (totalXOffset + bodyLength + headOffset) * pixSizeX,
        (legLength + legYOffset) / 32d + (headRadius + headYOffset) / 32d * CylinderSize.y60,
        -(totalXOffset + bodyLength + headOffset) * pixSizeZ
      )
      .toCylCoordsOffset,
    new Vector3f(0, math.Pi.toFloat / 2, math.Pi.toFloat / 2),
    setup = partsNBT.get("head").asObject()
  )
  private val body = new BasicEntityPart(
    makeHexBox(bodyRadius, 0, bodyLength),
    BlockCoords
      .Offset(
        totalXOffset * pixSizeX,
        (legLength + legYOffset) / 32d,
        -totalXOffset * pixSizeZ
      )
      .toCylCoordsOffset,
    new Vector3f(0, math.Pi.toFloat / 2, -math.Pi.toFloat / 2),
    setup = partsNBT.get("body").asObject()
  )
  private val frontRightLeg = new BasicEntityPart(
    makeHexBox(legRadius, 0, legLength),
    BlockCoords
      .Offset(
        (totalXOffset + bodyLength - legRadius) * pixSizeX,
        legLength / 32d,
        legOffset / 32d - (totalXOffset + bodyLength - legRadius) * pixSizeZ
      )
      .toCylCoordsOffset,
    new Vector3f(math.Pi.toFloat, 0, 0),
    setup = partsNBT.get("front_right_leg").asObject()
  )
  private val frontLeftLeg = new BasicEntityPart(
    makeHexBox(legRadius, 0, legLength),
    BlockCoords
      .Offset(
        (totalXOffset + bodyLength - legRadius) * pixSizeX,
        legLength / 32d,
        -legOffset / 32d - (totalXOffset + bodyLength - legRadius) * pixSizeZ
      )
      .toCylCoordsOffset,
    new Vector3f(math.Pi.toFloat, 0, 0),
    setup = partsNBT.get("front_left_leg").asObject()
  )
  private val backRightLeg = new BasicEntityPart(
    makeHexBox(legRadius, 0, legLength),
    BlockCoords
      .Offset(
        (totalXOffset + legRadius) * pixSizeX,
        legLength / 32d,
        legOffset / 32d - (totalXOffset + legRadius) * pixSizeZ
      )
      .toCylCoordsOffset,
    new Vector3f(math.Pi.toFloat, 0, 0),
    setup = partsNBT.get("back_right_leg").asObject()
  )
  private val backLeftLeg = new BasicEntityPart(
    makeHexBox(legRadius, 0, legLength),
    BlockCoords
      .Offset(
        (totalXOffset + legRadius) * pixSizeX,
        legLength / 32d,
        -legOffset / 32d - (totalXOffset + legRadius) * pixSizeZ
      )
      .toCylCoordsOffset,
    new Vector3f(math.Pi.toFloat, 0, 0),
    setup = partsNBT.get("back_left_leg").asObject()
  )

  override val parts: Seq[EntityPart] =
    Seq(head, body, frontRightLeg, frontLeftLeg, backRightLeg, backLeftLeg)
  var time = 0f
  override def tick(): Unit = {
    // val pi = math.Pi.toFloat

    time += 1f / 60

    val phase = time * 2 * math.Pi

    frontRightLeg.rotation.z = -0.5f * math.sin(phase).toFloat
    frontLeftLeg.rotation.z = 0.5f * math.sin(phase).toFloat

    backRightLeg.rotation.z = 0.5f * math.sin(phase).toFloat
    backLeftLeg.rotation.z = -0.5f * math.sin(phase).toFloat

    // val neckSin = clamp(math.sin(phase / 2).toFloat * 2, -1, 1)
    // head.rotation.z = lerp(neckSin, -1, 1, pi / 2 - pi / 4, pi / 2)
    // head.rotation.x = lerp(neckSin, -1, 1, 0, pi / 4)
  }

  override def texture: TextureSingle =
    TextureSingle.getTexture(
      "textures/entities/" + setup.getString("texture", "")
    )

  override def texSize: Int = 256
}
