package com.martomate.hexacraft.world.entity

import com.martomate.hexacraft.world.block.HexBox

import org.joml.Matrix4f

trait EntityPart {
  def baseTransform: Matrix4f
  def transform: Matrix4f
  def box: HexBox
  def texture(side: Int): Int
  def textureOffset(side: Int): (Int, Int) = (0, 0)
  def textureSize(side: Int): (Int, Int)
}
