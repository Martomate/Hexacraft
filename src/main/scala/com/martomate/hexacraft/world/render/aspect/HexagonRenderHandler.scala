package com.martomate.hexacraft.world.render.aspect

import com.martomate.hexacraft.renderer.Shader
import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld
import com.martomate.hexacraft.world.render.buffer.BufferHandler
import com.martomate.hexacraft.world.render.buffer.vao.BlockVAORenderBufferFactory

import java.nio.ByteBuffer

class HexagonRenderHandler(topShader: Shader, sideShader: Shader) {

  private def bufferHandlerMaker(s: Int): BufferHandler[_] =
    new BufferHandler(1000000, new BlockVAORenderBufferFactory(s))

  private val sideHandlers: IndexedSeq[RenderAspectHandler] =
    IndexedSeq.tabulate(8)(s => new RenderAspectHandler(bufferHandlerMaker(s)))

  def render(): Unit =
    for side <- 0 until 8 do
      val sh = if side < 2 then topShader else sideShader
      sh.enable()
      sh.setUniform1i("side", side)
      sideHandlers(side).render()

  def setChunkContent(coords: ChunkRelWorld, content: Option[IndexedSeq[ByteBuffer]]): Unit =
    content match
      case Some(cc) => setChunkContent(coords, cc)
      case None     => clearChunkContent(coords)

  def setChunkContent(coords: ChunkRelWorld, content: IndexedSeq[ByteBuffer]): Unit =
    for s <- 0 until 8 do sideHandlers(s).setChunkContent(coords, Option(content(s)))

  def clearChunkContent(coords: ChunkRelWorld): Unit =
    for s <- 0 until 8 do sideHandlers(s).setChunkContent(coords, None)

  def unload(): Unit = sideHandlers.foreach(_.unload())
}
