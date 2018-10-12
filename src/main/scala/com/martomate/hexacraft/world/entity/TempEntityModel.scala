package com.martomate.hexacraft.world.entity

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.HexBox
import com.martomate.hexacraft.world.coord.fp.{BlockCoords, CylCoords}
import org.joml.Vector3f

class TempEntityModel(pos: CylCoords, box: HexBox) extends EntityModel {
  private val theBox = new TempEntityPart(box, pos, new Vector3f)

  override val parts: Seq[EntityPart] = Seq(theBox)
}

class PlayerEntityModel(pos: CylCoords, box: HexBox) extends EntityModel {
  import pos.cylSize.impl

  def makeHexBox(r: Int, b: Float, h: Int): HexBox =
    new HexBox(r / 32f * 0.5f, b / 32f * 0.5f, (h + b) / 32f * 0.5f)

  private val head = new TempEntityPart(
    makeHexBox(8, -5, 10),
    BlockCoords(0, 40f / 32 + 8f / 32 * CylinderSize.y60, 0).toCylCoords,
    new Vector3f(0, math.Pi.toFloat / 2, math.Pi.toFloat / 2)
  )
  private val leftBodyhalf = new TempEntityPart(
    makeHexBox(4, 0, 20),
    BlockCoords(0, 20f / 32, -0.5f / 8).toCylCoords,
    new Vector3f(0, 0, 0)
  )
  private val rightBodyhalf = new TempEntityPart(
    makeHexBox(4, 0, 20),
    BlockCoords(0, 20f / 32, 0.5f / 8).toCylCoords,
    new Vector3f(0, 0, 0)
  )
  private val rightArm = new TempEntityPart(
    makeHexBox(4, -4 * CylinderSize.y60.toFloat, 20),
    BlockCoords(0, (40f - 4 * CylinderSize.y60.toFloat) / 32, 0.5f / 4 + 0.5f / 8).toCylCoords,
    new Vector3f(math.Pi.toFloat * -6 / 6, 0, 0)
  )
  private val leftArm = new TempEntityPart(
    makeHexBox(4, -4 * CylinderSize.y60.toFloat, 20),
    BlockCoords(0, (40f - 4 * CylinderSize.y60.toFloat) / 32, -0.5f / 4 - 0.5f / 8).toCylCoords,
    new Vector3f(math.Pi.toFloat * 6 / 6, 0, 0)
  )
  private val rightLeg = new TempEntityPart(
    makeHexBox(4, 0, 20),
    BlockCoords(0, 20f / 32, 0.5f / 8).toCylCoords,
    new Vector3f(math.Pi.toFloat, 0, 0)
  )
  private val leftLeg = new TempEntityPart(
    makeHexBox(4, 0, 20),
    BlockCoords(0, 20f / 32, -0.5f / 8).toCylCoords,
    new Vector3f(math.Pi.toFloat, 0, 0)
  )

  override val parts: Seq[EntityPart] = Seq(head, leftBodyhalf, rightBodyhalf, rightArm, leftArm, rightLeg, leftLeg)
var time = 0f
  def tempTick(): Unit = {
    time += 0.03f

    rightArm.rotation.z = -0.5f * math.sin(time).toFloat
    leftArm.rotation.z =  0.5f * math.sin(time).toFloat

    rightArm.rotation.x += -0.05f * math.sin(time).toFloat
    leftArm.rotation.x +=  0.05f * math.sin(time).toFloat

    rightLeg.rotation.z =  0.5f * math.sin(time).toFloat
    leftLeg.rotation.z = -0.5f * math.sin(time).toFloat
  }
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
  def tempTick(): Unit = {
    box2.rotation.y += 0.01f
    box3.rotation.z += 0.01f
    box1.rotation.y = time
    box4.rotation.y = time
    box4.rotation.z = 0.3f * math.cos(time).toFloat
    box4.rotation.x = 0.3f * math.sin(time).toFloat

    time += 0.01f
  }
}
