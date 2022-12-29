package com.martomate.hexacraft.world.entity.horse

import com.martomate.hexacraft.renderer.TextureSingle
import com.martomate.hexacraft.util.{CylinderSize, MathUtils}
import com.martomate.hexacraft.world.block.HexBox
import com.martomate.hexacraft.world.coord.fp.BlockCoords
import com.martomate.hexacraft.world.entity.{EntityModel, EntityPart}
import com.martomate.hexacraft.world.entity.base.BasicEntityPart

import com.eclipsesource.json.JsonObject
import org.joml.Vector3f

class HorseEntityModel(
    val neck: BasicEntityPart,
    val head: BasicEntityPart,
    val body: BasicEntityPart,
    val frontRightLeg: BasicEntityPart,
    val frontLeftLeg: BasicEntityPart,
    val backRightLeg: BasicEntityPart,
    val backLeftLeg: BasicEntityPart,
    val textureName: String
) extends EntityModel:
  override val parts: Seq[EntityPart] = Seq(neck, head, body, frontRightLeg, frontLeftLeg, backRightLeg, backLeftLeg)

  private val animation = new HorseAnimation(this)

  override def tick(): Unit = animation.tick()

  override def texture: TextureSingle =
    TextureSingle.getTexture(
      "textures/entities/" + textureName
    )

  override def texSize: Int = 256

class HorseAnimation(model: HorseEntityModel):
  var time = 0f

  def tick(): Unit =
    val pi = math.Pi.toFloat

    time += 1f / 60

    val phase = time * 2 * math.Pi

    model.frontRightLeg.rotation.z = -0.5f * math.sin(phase).toFloat
    model.frontLeftLeg.rotation.z = 0.5f * math.sin(phase).toFloat

    model.backRightLeg.rotation.z = 0.5f * math.sin(phase).toFloat
    model.backLeftLeg.rotation.z = -0.5f * math.sin(phase).toFloat

    val neckSin = MathUtils.clamp(math.sin(phase / 2).toFloat * 2, -1, 1)
    model.neck.rotation.z = -pi / 2 + neckSin * pi / 4
    model.head.rotation.x = MathUtils.remap(neckSin, -1, 1, pi / 4, pi / 2)

object HorseEntityModel:
  def fromJson(setup: JsonObject): HorseEntityModel =
    val partsNBT = setup.get("parts").asObject()

    def makeHexBox(r: Int, b: Int, h: Int): HexBox =
      new HexBox(r / 32f * 0.5f, b / 32f * 0.5f, (h + b) / 32f * 0.5f)

    val legLength = 32
    val legRadius = 6
    val bodyLength = 48
    val bodyRadius = 16
    val neckDepth = 18
    val neckRadius = 6
    val headDepth = 24
    val headRadius = 6

    val legOffset = bodyRadius * 0.25f / CylinderSize.y60
    val legYOffset = 3

    val pixSizeX: Double = 0.5 / CylinderSize.y60 / CylinderSize.y60 / 32
    val pixSizeZ: Double = pixSizeX * 0.5

    val neck = new BasicEntityPart(
      makeHexBox(neckRadius, neckRadius + 2, neckDepth),
      BlockCoords
        .Offset(
          (bodyLength - neckRadius * 2 + 2) * pixSizeX,
          (legLength + legYOffset) / 32d + (bodyRadius - neckRadius * 2 - 2) / 32d * CylinderSize.y60,
          -(bodyLength - neckRadius * 2 + 2) * pixSizeZ
        )
        .toCylCoordsOffset,
      new Vector3f(
        0,
        math.Pi.toFloat / 2,
        -math.Pi.toFloat / 2 + math.Pi.toFloat / 4
      ),
      setup = partsNBT.get("body").asObject()
    )
    val head = new BasicEntityPart(
      makeHexBox(headRadius, -neckRadius, headDepth),
      BlockCoords.Offset(0, neckDepth / 32d, 0).toCylCoordsOffset,
      new Vector3f(math.Pi.toFloat / 4, 0, 0),
      parentPart = neck,
      setup = partsNBT.get("head").asObject()
    )
    val body = new BasicEntityPart(
      makeHexBox(bodyRadius, 0, bodyLength),
      BlockCoords.Offset(0, (legLength + legYOffset) / 32d, 0).toCylCoordsOffset,
      new Vector3f(0, math.Pi.toFloat / 2, -math.Pi.toFloat / 2),
      setup = partsNBT.get("body").asObject()
    )
    val frontRightLeg = new BasicEntityPart(
      makeHexBox(legRadius, 0, legLength),
      BlockCoords
        .Offset(
          (bodyLength - legRadius) * pixSizeX,
          legLength / 32d,
          legOffset / 32d - (bodyLength - legRadius) * pixSizeZ
        )
        .toCylCoordsOffset,
      new Vector3f(math.Pi.toFloat, 0, 0),
      setup = partsNBT.get("front_right_leg").asObject()
    )
    val frontLeftLeg = new BasicEntityPart(
      makeHexBox(legRadius, 0, legLength),
      BlockCoords
        .Offset(
          (bodyLength - legRadius) * pixSizeX,
          legLength / 32d,
          -legOffset / 32d - (bodyLength - legRadius) * pixSizeZ
        )
        .toCylCoordsOffset,
      new Vector3f(math.Pi.toFloat, 0, 0),
      setup = partsNBT.get("front_left_leg").asObject()
    )
    val backRightLeg = new BasicEntityPart(
      makeHexBox(legRadius, 0, legLength),
      BlockCoords
        .Offset(
          legRadius * pixSizeX,
          legLength / 32d,
          legOffset / 32d - legRadius * pixSizeZ
        )
        .toCylCoordsOffset,
      new Vector3f(math.Pi.toFloat, 0, 0),
      setup = partsNBT.get("back_right_leg").asObject()
    )
    val backLeftLeg = new BasicEntityPart(
      makeHexBox(legRadius, 0, legLength),
      BlockCoords
        .Offset(
          legRadius / 32d * 0.5f / CylinderSize.y60 / CylinderSize.y60,
          legLength / 32d,
          -legOffset / 32d - legRadius * pixSizeZ
        )
        .toCylCoordsOffset,
      new Vector3f(math.Pi.toFloat, 0, 0),
      setup = partsNBT.get("back_left_leg").asObject()
    )

    new HorseEntityModel(
      neck,
      head,
      body,
      frontRightLeg,
      frontLeftLeg,
      backRightLeg,
      backLeftLeg,
      setup.getString("texture", "")
    )
