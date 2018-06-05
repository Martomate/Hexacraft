package hexacraft.world.render

import hexacraft.block.BlockState
import hexacraft.world.storage.Chunk

import scala.collection.mutable

class ChunkRenderer(chunk: Chunk) {
  private var blockRenderers: Option[BlockRendererCollection[BlockRenderer]] = None

  def updateContent(): Unit = {
    onlyKeepBlockRenderersIfChunkNotEmpty()

    if (!chunk.isEmpty) {
      val blocks = chunk.blocks.allBlocks
      val sidesToRender = Seq.fill(8)(new mutable.BitSet(16 * 16 * 16))
      val sidesCount = new Array[Int](8)
      for ((c, b) <- blocks) {
        for (s <- BlockState.neighborOffsets.indices) {
          val bs = chunk.neighborBlock(s, c)
          if (bs.blockType.isTransparent(bs, oppositeSide(s))) {
            sidesToRender(s)(c.value) = true
            sidesCount(s) += 1
            if (s > 1 && b.blockType.isTransparent(b, s)) {
              sidesToRender(0)(c.value) = true
              sidesCount(0) += 1
            } // render the top side
          }
        }
      }
      for (side <- 0 until 8) {
        blockRenderers.get.updateContent(side, sidesCount(side), buf => {
          for ((bCoords, block) <- blocks) {
            val coords = bCoords.withChunk(chunk.coords)
            if (sidesToRender(side)(bCoords.value)) {
              buf.putInt(coords.x)
              buf.putInt(coords.y)
              buf.putInt(coords.z)
              val blockType = block.blockType
              buf.putInt(blockType.blockTex(side))
              buf.putFloat(blockType.blockHeight(block))
            }
          }
        })
      }
    }
  }

  private def onlyKeepBlockRenderersIfChunkNotEmpty(): Unit = {
    if (chunk.isEmpty) {
      blockRenderers.foreach(_.unload())
      blockRenderers = None
    } else {
      if (blockRenderers.isEmpty)
        blockRenderers = Some(new BlockRendererCollection(s => new BlockRenderer(s, 0)))
    }
  }

  private def oppositeSide(s: Int) = {
    if (s < 2) 1 - s else (s - 2 + 3) % 3 + 2
  }

  def renderBlockSide(side: Int): Unit = blockRenderers.foreach(_.renderBlockSide(side))

  def unload(): Unit = {
    blockRenderers.foreach(_.unload())
  }
}
