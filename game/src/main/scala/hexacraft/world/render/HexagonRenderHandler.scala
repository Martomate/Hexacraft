package hexacraft.world.render

import hexacraft.renderer.GpuState
import hexacraft.shaders.BlockShader
import hexacraft.world.CylinderSize
import hexacraft.world.coord.ChunkRelWorld

import java.nio.ByteBuffer
import scala.collection.mutable

class HexagonRenderHandler(topShader: BlockShader, sideShader: BlockShader, gpuState: GpuState) {

  private def bufferHandlerMaker(s: Int): BufferHandler[?] =
    new BufferHandler(100000 * 3, BlockShader.bytesPerVertex(s), VaoRenderBuffer.Allocator(s, gpuState))

  private val sideHandlers: IndexedSeq[mutable.LongMap[RenderAspectHandler]] =
    IndexedSeq.tabulate(8)(s => new mutable.LongMap())

  def fragmentation: IndexedSeq[Float] = sideHandlers.map(a => a.values.map(_.fragmentation).sum / a.keys.size)

  def render(): Unit = {
    for side <- 0 until 8 do {
      val sh = if side < 2 then topShader else sideShader
      sh.enable()
      sh.setSide(side)
      for h <- sideHandlers(side).values do {
        h.render()
      }
    }
  }

  def update(
      chunksToClear: Seq[ChunkRelWorld],
      chunksToUpdate: Seq[(ChunkRelWorld, IndexedSeq[ByteBuffer])]
  ): Unit = {
    for s <- 0 until 8 do {
      given CylinderSize = CylinderSize(20) // not used

      val clear = chunksToClear.groupBy(c => ChunkRelWorld(c.X.toInt & ~7, c.Y.toInt & ~7, c.Z.toInt & ~7))
      val update =
        chunksToUpdate.groupBy((c, _) => ChunkRelWorld(c.X.toInt & ~7, c.Y.toInt & ~7, c.Z.toInt & ~7))

      for g <- (clear.keys ++ update.keys).toSet do {
        val h = sideHandlers(s).getOrElseUpdate(g.value, new RenderAspectHandler(bufferHandlerMaker(s)))
        h.update(clear.getOrElse(g, Seq()), update.getOrElse(g, Seq()).map((c, data) => c -> data(s)))
        if h.isEmpty then {
          h.unload()
          sideHandlers(s).remove(g.value)
        }
      }
    }
  }

  def unload(): Unit = {
    for hs <- sideHandlers do {
      for h <- hs.values do {
        h.unload()
      }
      hs.clear()
    }
  }
}
