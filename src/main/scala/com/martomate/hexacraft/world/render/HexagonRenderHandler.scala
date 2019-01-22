package com.martomate.hexacraft.world.render

import java.nio.ByteBuffer

import com.martomate.hexacraft.resource.Shader

class HexagonRenderHandler(topShader: Shader, sideShader: Shader) {

  def bufferHandlerMaker(s: Int): BufferHandler[_] = new VAOBufferHandler(new BlockVAORenderBufferFactory(s))

  private val sideHandlers: IndexedSeq[RenderAspectHandler] = IndexedSeq.tabulate(8)(s => new RenderAspectHandler(bufferHandlerMaker(s)))

  def render(): Unit = {
    for (side <- 0 until 8) {
      val sh = if (side < 2) topShader else sideShader
      sh.enable()
      sh.setUniform1i("side", side)
      sideHandlers(side).render()
    }
  }

  def setChunkContent(chunk: ChunkRenderer, content: Option[IndexedSeq[ByteBuffer]]): Unit = {
    for (s <- 0 until 8) sideHandlers(s).setChunkContent(chunk, content.flatMap(c => Option(c(s))))
  }

  def unload(): Unit = sideHandlers.foreach(_.unload())
}
