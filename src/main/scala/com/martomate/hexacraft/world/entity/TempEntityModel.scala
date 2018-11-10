package com.martomate.hexacraft.world.entity

import com.eclipsesource.json.JsonObject
import com.martomate.hexacraft.resource.TextureSingle
import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.HexBox
import com.martomate.hexacraft.world.coord.fp.{BlockCoords, CylCoords}
import org.joml.Vector3f

class TempEntityModel(pos: CylCoords, box: HexBox) extends EntityModel {
  private val theBox = new TempEntityPart(box, pos, new Vector3f)

  override val parts: Seq[EntityPart] = Seq(theBox)

  override def tick(): Unit = ()

  override def texture: TextureSingle = ???

  override def texSize: Int = ???
}

class PlayerEntityModel(setup: JsonObject)(implicit cylinderSize: CylinderSize) extends EntityModel {
  private val partsNBT = setup.get("parts").asObject()

  def makeHexBox(r: Int, b: Float, h: Int): HexBox =
    new HexBox(r / 32f * 0.5f, b / 32f * 0.5f, (h + b) / 32f * 0.5f)

  private val legLength = 40
  private val legRadius = 8
  private val bodyLength = 40
  private val bodyRadius = 8
  private val armLength = 40
  private val armRadius = 8
  private val headRadius = 16
  private val headDepth = 20

  private val head = new TempEntityPart(
    makeHexBox(headRadius, -headDepth / 2f, headDepth),
//    BlockCoords(0, (bodyLength + legLength) / 32f + headDepth / 2 / 32f, 0).toCylCoords,
    BlockCoords(0, (bodyLength + legLength) / 32f + headRadius / 32f * CylinderSize.y60, 0).toCylCoords,
    new Vector3f(0, math.Pi.toFloat / 2, math.Pi.toFloat / 2),
    setup = partsNBT.get("head").asObject()
  )
  private val leftBodyhalf = new TempEntityPart(
    makeHexBox(bodyRadius, 0, bodyLength),
    BlockCoords(0, legLength / 32f, -0.5f * bodyRadius / 32).toCylCoords,
    new Vector3f(0, 0, 0),
    setup = partsNBT.get("leftbody").asObject()
  )
  private val rightBodyhalf = new TempEntityPart(
    makeHexBox(bodyRadius, 0, bodyLength),
    BlockCoords(0, legLength / 32f, 0.5f * bodyRadius / 32).toCylCoords,
    new Vector3f(0, 0, 0),
    setup = partsNBT.get("rightbody").asObject()
  )
  private val rightArm = new TempEntityPart(
    makeHexBox(armRadius, -armRadius * CylinderSize.y60.toFloat, armLength),
    BlockCoords(0, (legLength + bodyLength - armRadius * CylinderSize.y60.toFloat) / 32f, 0.5f * 2 * bodyRadius / 32 + 0.5f * armRadius / 32).toCylCoords,
    new Vector3f(math.Pi.toFloat * -6 / 6, 0, 0),
    setup = partsNBT.get("rightarm").asObject()
  )
  private val leftArm = new TempEntityPart(
    makeHexBox(armRadius, -armRadius * CylinderSize.y60.toFloat, armLength),
    BlockCoords(0, (legLength + bodyLength - armRadius * CylinderSize.y60.toFloat) / 32f, -0.5f * 2 * bodyRadius / 32 - 0.5f * armRadius / 32).toCylCoords,
    new Vector3f(math.Pi.toFloat * 6 / 6, 0, 0),
    setup = partsNBT.get("leftarm").asObject()
  )
  private val rightLeg = new TempEntityPart(
    makeHexBox(legRadius, 0, legLength),
    BlockCoords(0, legLength / 32f, 0.5f * legRadius / 32).toCylCoords,
    new Vector3f(math.Pi.toFloat, 0, 0),
    setup = partsNBT.get("rightleg").asObject()
  )
  private val leftLeg = new TempEntityPart(
    makeHexBox(legRadius, 0, legLength),
    BlockCoords(0, legLength / 32f, -0.5f * legRadius / 32).toCylCoords,
    new Vector3f(math.Pi.toFloat, 0, 0),
    setup = partsNBT.get("leftleg").asObject()
  )

  override val parts: Seq[EntityPart] = Seq(head, leftBodyhalf, rightBodyhalf, rightArm, leftArm, rightLeg, leftLeg)
var time = 0f
  override def tick(): Unit = {
    time += 0.03f

    rightArm.rotation.z = -0.5f * math.sin(time).toFloat
    leftArm.rotation.z =  0.5f * math.sin(time).toFloat

    rightArm.rotation.x += -0.03f * math.sin(time).toFloat
    leftArm.rotation.x +=  0.03f * math.sin(time).toFloat

    rightLeg.rotation.z =  0.5f * math.sin(time).toFloat
    leftLeg.rotation.z = -0.5f * math.sin(time).toFloat
  }

  override def texture: TextureSingle = TextureSingle.getTexture("textures/entities/" + setup.getString("texture", ""))

  override def texSize: Int = 256
}


class TempComplexEntityModel(pos: CylCoords, box: HexBox) extends EntityModel {
  import pos.cylSize.impl

  private val box1 = new TempEntityPart(
    new HexBox(0.5f, 0, 0.25f),
    BlockCoords(0, 1, 0).toCylCoords,
    new Vector3f(0, 0, 0)
  )
  private val box2 = new TempEntityPart(
    new HexBox(0.125f, 0, 0.5f),
    BlockCoords(0, 1, 0.5f).toCylCoords,
    new Vector3f(-math.Pi.toFloat * 7 / 6, 0, 0)
  )
  private val box3 = new TempEntityPart(
    new HexBox(0.125f, 0, 0.5f),
    BlockCoords(0, 1, -0.5f).toCylCoords,
    new Vector3f(math.Pi.toFloat * 7 / 6, 0, 0)
  )
  private val box4 = new TempEntityPart(
    new HexBox(0.25f, 0, 0.6f),
    BlockCoords(0, 0, 0).toCylCoords,
    new Vector3f(0, 0, 0)
  )

  override val parts: Seq[EntityPart] = Seq(box1, box2, box3, box4)
  var time = 0f
  override def tick(): Unit = {
    box2.rotation.y += 0.01f
    box3.rotation.z += 0.01f
    box1.rotation.y = time
    box4.rotation.y = time
    box4.rotation.z = 0.3f * math.cos(time).toFloat
    box4.rotation.x = 0.3f * math.sin(time).toFloat

    time += 0.01f
  }

  override def texture: TextureSingle = ???

  override def texSize: Int = ???
}
