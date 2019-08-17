package com.martomate.hexacraft.world.render.buffer

import java.nio.ByteBuffer

trait RenderBuffer[B <: RenderBuffer[B]] {
  def set(start: Int, length: Int, buf: ByteBuffer): Unit
  def copyTo(buffer: B, fromIdx: Int, toIdx: Int, len: Int): Unit

  def render(length: Int): Unit

  def unload(): Unit
}
