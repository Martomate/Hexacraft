package com.martomate.hexacraft.world.render.buffer.vao

import com.martomate.hexacraft.world.render.buffer.{BufferHandler, RenderBufferFactory}

class VAOBufferHandler(bufferFactory: RenderBufferFactory[VAORenderBuffer]) extends BufferHandler(bufferFactory) {
  override protected def copyInternal(fromBuffer: Int, fromIdx: Int, toBuffer: Int, toIdx: Int, len: Int): Unit = buffers(fromBuffer).copyFrom(buffers(toBuffer), fromIdx, toIdx, len)
}
