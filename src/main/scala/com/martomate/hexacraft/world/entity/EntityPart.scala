package com.martomate.hexacraft.world.entity

import com.martomate.hexacraft.world.block.HexBox
import com.martomate.hexacraft.world.coord.fp.{BlockCoords, CylCoords}
import org.joml.Matrix4f

trait EntityPart {
  def transform: Matrix4f
  def box: HexBox
  def texture(side: Int): Int
  def textureSize(side: Int): (Int, Int)
}

class TempEntityPart(pos: CylCoords, _box: HexBox) extends EntityPart {
  override val transform: Matrix4f = new Matrix4f().translate(pos.toVector3f)

  override val box: HexBox = _box

  override def texture(side: Int): Int = 1

  override def textureSize(side: Int): (Int, Int) = (32, 32)
}