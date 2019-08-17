package com.martomate.hexacraft.renderer

import java.nio.ByteBuffer

trait VertexData {
  def bytesPerVertex: Int

  def fill(buf: ByteBuffer): Unit
}
