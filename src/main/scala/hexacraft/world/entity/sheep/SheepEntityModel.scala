package hexacraft.world.entity.sheep

import hexacraft.renderer.TextureSingle
import hexacraft.world.CylinderSize
import hexacraft.world.block.HexBox
import hexacraft.world.coord.fp.BlockCoords
import hexacraft.world.entity.{EntityModel, EntityPart}
import hexacraft.world.entity.base.BasicEntityPart

import com.eclipsesource.json.JsonObject
import org.joml.Vector3f

class SheepEntityModel(
    val head: BasicEntityPart,
    val body: BasicEntityPart,
    val frontRightLeg: BasicEntityPart,
    val frontLeftLeg: BasicEntityPart,
    val backRightLeg: BasicEntityPart,
    val backLeftLeg: BasicEntityPart,
    val textureName: String
) extends EntityModel:
  override val parts: Seq[EntityPart] = Seq(head, body, frontRightLeg, frontLeftLeg, backRightLeg, backLeftLeg)

  private val animation = new SheepAnimation(this)

  override def tick(): Unit = animation.tick()

  override def texture: TextureSingle =
    TextureSingle.getTexture(
      "textures/entities/" + textureName
    )

class SheepAnimation(model: SheepEntityModel):
  var time = 0f

  def tick(): Unit =
    time += 1f / 60

    val phase = time * 2 * math.Pi

    model.frontRightLeg.rotation.z = -0.5f * math.sin(phase).toFloat
    model.frontLeftLeg.rotation.z = 0.5f * math.sin(phase).toFloat

    model.backRightLeg.rotation.z = 0.5f * math.sin(phase).toFloat
    model.backLeftLeg.rotation.z = -0.5f * math.sin(phase).toFloat

object SheepEntityModel:
  def fromJson(setup: JsonObject): SheepEntityModel =
    val partsNBT = setup.get("parts").asObject()

    def makeHexBox(r: Int, b: Float, h: Int): HexBox =
      new HexBox(r / 32f * 0.5f, b / 32f * 0.5f, (h + b) / 32f * 0.5f)

    val legLength = 32
    val legRadius = 6
    val bodyLength = 48
    val bodyRadius = 16
    val headDepth = 16
    val headRadius = 12

    val headOffset = 2
    val headYOffset = 2
    val legOffset = bodyRadius * 0.25f / CylinderSize.y60
    val legYOffset = 3

    val pixSizeX: Double = 0.5 / CylinderSize.y60 / CylinderSize.y60 / 32
    val pixSizeZ: Double = pixSizeX * 0.5

    val totalXOffset = -bodyLength / 2

    val head = new BasicEntityPart(
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
    val body = new BasicEntityPart(
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
    val frontRightLeg = new BasicEntityPart(
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
    val frontLeftLeg = new BasicEntityPart(
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
    val backRightLeg = new BasicEntityPart(
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
    val backLeftLeg = new BasicEntityPart(
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

    new SheepEntityModel(
      head,
      body,
      frontRightLeg,
      frontLeftLeg,
      backRightLeg,
      backLeftLeg,
      setup.getString("texture", "")
    )
