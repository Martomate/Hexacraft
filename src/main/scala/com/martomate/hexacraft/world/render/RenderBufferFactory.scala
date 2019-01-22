package com.martomate.hexacraft.world.render

trait RenderBufferFactory[T <: RenderBuffer] {
  def bytesPerInstance: Int
  def makeBuffer(maxInstances: Int): T
}
