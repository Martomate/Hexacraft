package hexacraft.world.render

import hexacraft.renderer.GpuState
import hexacraft.world.coord.ChunkRelWorld

import java.nio.ByteBuffer

class HexagonRenderHandler(topShader: BlockShader, sideShader: BlockShader, gpuState: GpuState) {

  private def bufferHandlerMaker(s: Int): BufferHandler[_] =
    new BufferHandler(1000000, BlockVao.bytesPerInstance(s), VaoRenderBuffer.Allocator(s, gpuState))

  private val sideHandlers: IndexedSeq[RenderAspectHandler] =
    IndexedSeq.tabulate(8)(s => new RenderAspectHandler(bufferHandlerMaker(s)))

  def fragmentation: IndexedSeq[Float] = sideHandlers.map(_.fragmentation)

  def render(): Unit = {
    for side <- 0 until 8 do {
      val sh = if side < 2 then topShader else sideShader
      sh.enable()
      sh.setSide(side)
      sideHandlers(side).render()
    }
  }

  def setChunkContent(coords: ChunkRelWorld, content: IndexedSeq[ByteBuffer]): Unit = {
    for s <- 0 until 8 do {
      sideHandlers(s).setChunkContent(coords, content(s))
    }
  }

  def clearChunkContent(coords: ChunkRelWorld): Unit = {
    for s <- 0 until 8 do {
      sideHandlers(s).clearChunkContent(coords)
    }
  }

  def unload(): Unit = {
    for h <- sideHandlers do {
      h.unload()
    }
  }
}
