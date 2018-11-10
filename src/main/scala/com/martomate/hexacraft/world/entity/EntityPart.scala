package com.martomate.hexacraft.world.entity

import com.eclipsesource.json.JsonObject
import com.martomate.hexacraft.world.block.HexBox
import com.martomate.hexacraft.world.coord.fp.CylCoords
import org.joml.{Matrix4f, Vector3f}

trait EntityPart {

  def transform: Matrix4f
  def box: HexBox
  def texture(side: Int): Int
  def textureOffset(side: Int): (Int, Int) = (0, 0)
  def textureSize(side: Int): (Int, Int)
}

class TempEntityPart(override val box: HexBox, pos: CylCoords, val rotation: Vector3f, setup: JsonObject = new JsonObject) extends EntityPart {
  private val boxRadius = (box.radius * 32 / 0.5f).round
  private val boxHeight = ((box.top - box.bottom) * 32 / 0.5f).round

  private val textureBaseOffset = {
    val tex = setup.get("textures").asObject()
    val defTag = tex.get("default")
    if (defTag != null) {
      val texInfo = defTag.asObject()
      (texInfo.get("xoff").asInt(), texInfo.get("yoff").asInt())
    } else (0, 0)
  }

  override def transform: Matrix4f = new Matrix4f()
    .translate(pos.toVector3f)
    .rotateZ(rotation.z)
    .rotateX(rotation.x)
    .rotateY(rotation.y)
    .translate(0, box.bottom, 0)
    .scale(new Vector3f(box.radius, box.top - box.bottom, box.radius))

  override def texture(side: Int): Int = {
    val offset = if (side < 2) 0x12345 else 0
    val texID = 4
    offset << 12 | texID
  }

  override def textureOffset(side: Int): (Int, Int) = {
    val add = side match {
      case 0 => (0, 0)
      case 1 => (0, boxRadius + boxHeight)
      case _ => ((side - 2) * boxRadius, boxRadius)
    }
    (textureBaseOffset._1 + add._1, textureBaseOffset._2 + add._2)
  }

  override def textureSize(side: Int): (Int, Int) =
    if (side < 2) (boxRadius, boxRadius)
    else (boxRadius, boxHeight)
}