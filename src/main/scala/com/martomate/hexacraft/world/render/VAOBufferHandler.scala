package com.martomate.hexacraft.world.render

class VAOBufferHandler(bufferFactory: RenderBufferFactory[VAORenderBuffer]) extends BufferHandler(bufferFactory) {
  override protected def copyInternal(fromBuffer: Int, fromIdx: Int, toBuffer: Int, toIdx: Int, len: Int): Unit = buffers(fromBuffer).copyFrom(buffers(toBuffer), fromIdx, toIdx, len)
}
