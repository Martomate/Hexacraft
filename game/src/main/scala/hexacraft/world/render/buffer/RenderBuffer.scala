package hexacraft.world.render.buffer

import java.nio.ByteBuffer

object RenderBuffer {
  trait Allocator[T <: RenderBuffer[T]] {
    def allocate(instances: Int): T

    def copy(from: T, to: T, fromIdx: Int, toIdx: Int, len: Int): Unit
  }
}

trait RenderBuffer[B <: RenderBuffer[B]] {
  def set(start: Int, buf: ByteBuffer): Unit

  def render(length: Int): Unit

  def unload(): Unit
}
