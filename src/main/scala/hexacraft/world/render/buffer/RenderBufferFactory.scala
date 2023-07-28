package hexacraft.world.render.buffer

trait RenderBufferFactory[T <: RenderBuffer[T]] {
  def bytesPerInstance: Int
  def makeBuffer(maxInstances: Int): T
}
