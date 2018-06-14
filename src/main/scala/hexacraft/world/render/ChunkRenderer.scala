package hexacraft.world.render

import hexacraft.block.{BlockAir, BlockState}
import hexacraft.world.coord.BlockRelChunk
import hexacraft.world.storage.Chunk

import scala.collection.mutable

class ChunkRenderer(chunk: Chunk) {
  private var blockRenderers: Option[BlockRendererCollection[BlockRenderer]] = None

  private val brightness: Array[Byte] = new Array(16*16*16)
  private var brightnessInitialized: Boolean = false

  def updateContent(): Unit = {
    onlyKeepBlockRenderersIfChunkNotEmpty()

    if (!chunk.isEmpty) {
      val blocks = chunk.blocks.allBlocks

      if (!brightnessInitialized) {
        brightnessInitialized = true
        val lights = mutable.HashMap.empty[BlockRelChunk, BlockState]

        for ((c, b) <- blocks) {
          if (b.blockType.lightEmitted != 0) lights(c) = b
        }

        LightPropagator.initBrightnesses(chunk, lights)
      }

      val sidesToRender = Seq.fill(8)(new mutable.BitSet(16 * 16 * 16))
      val sideBrightness = Seq.fill(8)(new Array[Float](16*16*16))
      val sidesCount = new Array[Int](8)
      for ((c, b) <- blocks) {
        for (s <- BlockState.neighborOffsets.indices) {
          val (c2, neigh) = chunk.neighbor(s, c)
          val bs = neigh.map(_.getBlock(c2)).getOrElse(BlockAir.State)
          if (bs.blockType.isTransparent(bs, oppositeSide(s))) {
            sidesToRender(s)(c.value) = true
            sideBrightness(s)(c.value) = neigh.map(_.renderer.getBrightness(c2)).getOrElse(0f)
            sidesCount(s) += 1
            if (s > 1 && b.blockType.isTransparent(b, s)) {
              sidesToRender(0)(c.value) = true
              sidesCount(0) += 1
            } // render the top side
          }
        }
      }

      for (side <- 0 until 8) {
        blockRenderers.get.updateContent(side, sidesCount(side)) {buf =>
          for ((bCoords, block) <- blocks) {
            val coords = bCoords.withChunk(chunk.coords)
            if (sidesToRender(side)(bCoords.value)) {
              buf.putInt(coords.x)
              buf.putInt(coords.y)
              buf.putInt(coords.z)
              val blockType = block.blockType
              buf.putInt(blockType.blockTex(side))
              buf.putFloat(blockType.blockHeight(block))
              buf.putFloat(sideBrightness(side)(bCoords.value))
            }
          }
        }
      }
    } else if (!brightnessInitialized) {
      brightnessInitialized = true
      LightPropagator.initBrightnesses(chunk, mutable.HashMap.empty)
    }
  }



  def setSunlight(coords: BlockRelChunk, value: Int): Unit = {
    brightness(coords.value) = (brightness(coords.value) & 0xf | value << 4).toByte
  }

  def getSunlight(coords: BlockRelChunk): Byte = {
    ((brightness(coords.value) >> 4) & 0xf).toByte
  }

  def setTorchlight(coords: BlockRelChunk, value: Int): Unit = {
    brightness(coords.value) = (brightness(coords.value) & 0xf0 | value).toByte
  }

  def getTorchlight(coords: BlockRelChunk): Byte = {
    (brightness(coords.value) & 0xf).toByte
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

  def getBrightness(block: BlockRelChunk): Float = {
    math.min((getTorchlight(block) + getSunlight(block)) / 15f, 1.0f)
  }

  def unload(): Unit = {
    blockRenderers.foreach(_.unload())
  }
}
