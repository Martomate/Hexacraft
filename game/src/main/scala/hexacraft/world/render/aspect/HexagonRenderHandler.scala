package hexacraft.world.render.aspect

import hexacraft.world.coord.integer.ChunkRelWorld
import hexacraft.world.render.{BlockShader, BlockVao}
import hexacraft.world.render.buffer.{BufferHandler, VaoRenderBuffer}

import java.nio.ByteBuffer

class HexagonRenderHandler(topShader: BlockShader, sideShader: BlockShader) {

  private def bufferHandlerMaker(s: Int): BufferHandler[_] =
    new BufferHandler(1000000, BlockVao.bytesPerInstance(s), VaoRenderBuffer.Allocator(s))

  private val sideHandlers: IndexedSeq[RenderAspectHandler] =
    IndexedSeq.tabulate(8)(s => new RenderAspectHandler(bufferHandlerMaker(s)))

  def fragmentation: IndexedSeq[Float] = sideHandlers.map(_.fragmentation)

  def render(): Unit =
    for side <- 0 until 8 do
      val sh = if side < 2 then topShader else sideShader
      sh.enable()
      sh.setSide(side)
      sideHandlers(side).render()

  def setChunkContent(coords: ChunkRelWorld, content: IndexedSeq[ByteBuffer]): Unit =
    for s <- 0 until 8 do sideHandlers(s).setChunkContent(coords, Option(content(s)))

  def clearChunkContent(coords: ChunkRelWorld): Unit =
    for s <- 0 until 8 do sideHandlers(s).setChunkContent(coords, None)

  def unload(): Unit = sideHandlers.foreach(_.unload())
}
