package com.martomate.hexacraft.resource

trait Texture {
  def bind(): Unit
}

case class TextureToLoad(pixels: Array[Int])
