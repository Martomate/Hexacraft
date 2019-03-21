package com.martomate.hexacraft.world.render.buffer

trait RenderBufferFactory[T <: RenderBuffer] {
  def bytesPerInstance: Int
  def makeBuffer(maxInstances: Int): T
}
