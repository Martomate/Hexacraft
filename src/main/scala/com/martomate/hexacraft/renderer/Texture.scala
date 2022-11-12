package com.martomate.hexacraft.renderer

trait Texture {
  def bind(): Unit
}

case class TextureToLoad(pixels: Array[Int])
