package com.martomate.hexacraft.world.render

import java.nio.ByteBuffer

trait RenderBuffer {
  def set(start: Int, length: Int, buf: ByteBuffer): Unit

  def render(length: Int): Unit
}
