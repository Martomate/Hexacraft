package com.martomate.hexacraft.world.entity

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

class TempEntityPart(override val box: HexBox, pos: CylCoords, val rotation: Vector3f) extends EntityPart {
  override def transform: Matrix4f = new Matrix4f()
    .translate(pos.toVector3f)
    .rotateZ(rotation.z)
    .rotateX(rotation.x)
    .rotateY(rotation.y)
    .translate(0, box.bottom, 0)
    .scale(new Vector3f(box.radius, box.top - box.bottom, box.radius))

  override def texture(side: Int): Int = 1

  override def textureOffset(side: Int): (Int, Int) = {
    val texSize = textureSize(side)
    (32 - texSize._1, 32 - texSize._2)
  }

  override def textureSize(side: Int): (Int, Int) =
    if (side < 2) ((box.radius * 32 / 0.5f).round, (box.radius * 32 / 0.5f).round)
    else ((box.radius * 32 / 0.5f).round, ((box.top - box.bottom) * 32 / 0.5f).round)
}