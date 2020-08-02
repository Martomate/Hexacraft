package com.martomate.hexacraft.world.entity.horse

import com.eclipsesource.json.JsonObject
import com.martomate.hexacraft.resource.TextureSingle
import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.HexBox
import com.martomate.hexacraft.world.coord.fp.BlockCoords
import com.martomate.hexacraft.world.entity.base.BasicEntityPart
import com.martomate.hexacraft.world.entity.{EntityModel, EntityPart}
import org.joml.Vector3f

class HorseEntityModel(setup: JsonObject)(implicit cylinderSize: CylinderSize)
    extends EntityModel {
  private val partsNBT = setup.get("parts").asObject()

  def makeHexBox(r: Int, b: Float, h: Int): HexBox =
    new HexBox(r / 32f * 0.5f, b / 32f * 0.5f, (h + b) / 32f * 0.5f)

  private val legLength = 32
  private val legRadius = 6
  private val bodyLength = 48
  private val bodyRadius = 16
  private val neckDepth = 18
  private val neckRadius = 6
  private val headDepth = 24
  private val headRadius = 6

  private val legOffset = bodyRadius * 0.25f / CylinderSize.y60
  private val legYOffset = 3

  private val pixSizeX: Double = 0.5 / CylinderSize.y60 / CylinderSize.y60 / 32
  private val pixSizeZ: Double = pixSizeX * 0.5

  private val neck = new BasicEntityPart(
    makeHexBox(neckRadius, neckRadius + 2, neckDepth),
    BlockCoords(
      (bodyLength - neckRadius * 2 + 2) * pixSizeX,
      (legLength + legYOffset) / 32d + (bodyRadius - neckRadius * 2 - 2) / 32d * CylinderSize.y60,
      -(bodyLength - neckRadius * 2 + 2) * pixSizeZ,
      fixZ = false
    ).toCylCoords,
    new Vector3f(
      0,
      math.Pi.toFloat / 2,
      -math.Pi.toFloat / 2 + math.Pi.toFloat / 4
    ),
    setup = partsNBT.get("body").asObject()
  )
  private val head = new BasicEntityPart(
    makeHexBox(headRadius, -neckRadius, headDepth),
    BlockCoords(0, (neckDepth) / 32d, 0, fixZ = false).toCylCoords,
    new Vector3f(math.Pi.toFloat / 4, 0, 0),
    parentPart = neck,
    setup = partsNBT.get("head").asObject()
  )
  private val body = new BasicEntityPart(
    makeHexBox(bodyRadius, 0, bodyLength),
    BlockCoords(0, (legLength + legYOffset) / 32d, 0, fixZ = false).toCylCoords,
    new Vector3f(0, math.Pi.toFloat / 2, -math.Pi.toFloat / 2),
    setup = partsNBT.get("body").asObject()
  )
  private val frontRightLeg = new BasicEntityPart(
    makeHexBox(legRadius, 0, legLength),
    BlockCoords(
      (bodyLength - legRadius) * pixSizeX,
      legLength / 32d,
      legOffset / 32d - (bodyLength - legRadius) * pixSizeZ,
      fixZ = false
    ).toCylCoords,
    new Vector3f(math.Pi.toFloat, 0, 0),
    setup = partsNBT.get("front_right_leg").asObject()
  )
  private val frontLeftLeg = new BasicEntityPart(
    makeHexBox(legRadius, 0, legLength),
    BlockCoords(
      (bodyLength - legRadius) * pixSizeX,
      legLength / 32d,
      -legOffset / 32d - (bodyLength - legRadius) * pixSizeZ,
      fixZ = false
    ).toCylCoords,
    new Vector3f(math.Pi.toFloat, 0, 0),
    setup = partsNBT.get("front_left_leg").asObject()
  )
  private val backRightLeg = new BasicEntityPart(
    makeHexBox(legRadius, 0, legLength),
    BlockCoords(
      legRadius * pixSizeX,
      legLength / 32d,
      legOffset / 32d - legRadius * pixSizeZ,
      fixZ = false
    ).toCylCoords,
    new Vector3f(math.Pi.toFloat, 0, 0),
    setup = partsNBT.get("back_right_leg").asObject()
  )
  private val backLeftLeg = new BasicEntityPart(
    makeHexBox(legRadius, 0, legLength),
    BlockCoords(
      legRadius / 32d * 0.5f / CylinderSize.y60 / CylinderSize.y60,
      legLength / 32d,
      -legOffset / 32d - legRadius * pixSizeZ,
      fixZ = false
    ).toCylCoords,
    new Vector3f(math.Pi.toFloat, 0, 0),
    setup = partsNBT.get("back_left_leg").asObject()
  )

  override val parts: Seq[EntityPart] = Seq(
    neck,
    head,
    body,
    frontRightLeg,
    frontLeftLeg,
    backRightLeg,
    backLeftLeg
  )
  var time = 0f
  override def tick(): Unit = {
    val pi = math.Pi.toFloat

    time += 1f / 60

    val phase = time * 2 * math.Pi

    frontRightLeg.rotation.z = -0.5f * math.sin(phase).toFloat
    frontLeftLeg.rotation.z = 0.5f * math.sin(phase).toFloat

//    rightArm.rotation.x += -0.03f * math.sin(time).toFloat
//    leftArm.rotation.x +=  0.03f * math.sin(time).toFloat

    backRightLeg.rotation.z = 0.5f * math.sin(phase).toFloat
    backLeftLeg.rotation.z = -0.5f * math.sin(phase).toFloat

    val neckSin = clamp(math.sin(phase / 2).toFloat * 2, -1, 1)
    neck.rotation.z = -pi / 2 + neckSin * pi / 4
    head.rotation.x = lerp(neckSin, -1, 1, pi / 4, pi / 2)
  }

  private def lerp(a: Float,
                   loIn: Float,
                   hiIn: Float,
                   loOut: Float,
                   hiOut: Float): Float =
    (a - loIn) / (hiIn - loIn) * (hiOut - loOut) + loOut

  private def clamp(a: Float, lo: Float, hi: Float): Float =
    if (a < lo) lo else if (a > hi) hi else a

  override def texture: TextureSingle =
    TextureSingle.getTexture(
      "textures/entities/" + setup.getString("texture", "")
    )

  override def texSize: Int = 256
}
