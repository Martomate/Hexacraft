package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.chunk.{ChunkCache, IChunk}
import com.martomate.hexacraft.world.coord.CoordUtils
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.coord.integer.BlockRelWorld
import com.martomate.hexacraft.world.storage.LocalBlockState
import com.martomate.hexacraft.world.worldlike.BlocksInWorld
import org.joml.{Matrix4f, Vector4f}
import org.lwjgl.BufferUtils

import java.nio.ByteBuffer

class ChunkRendererImpl(chunk: IChunk, world: BlocksInWorld)(implicit cylSize: CylinderSize) extends ChunkRenderer {
  private val opaqueDeterminer: ChunkOpaqueDeterminer = new ChunkOpaqueDeterminerSimple(chunk.coords, chunk)

  def canGetToSide(fromSide: Int, toSide: Int): Boolean = opaqueDeterminer.canGetToSide(fromSide, toSide)

  private var renderData: ChunkRenderData = _
  def getRenderData: ChunkRenderData = renderData

  def updateContent(): Unit = {
    val buffers: Array[ByteBuffer] = Array.ofDim(8)

    if (!chunk.hasNoBlocks) {
      val blocks = chunk.blocks.allBlocks

      if (!chunk.lighting.initialized) {
        chunk.lighting.init(blocks)
      }

      val chunkCache = new ChunkCache(world)

      val sidesToRender = Array.tabulate[java.util.BitSet](8)(_ => new java.util.BitSet(16 * 16 * 16))
      val sideBrightness = Array.ofDim[Float](8, 16 * 16 * 16)
      val sidesCount = Array.ofDim[Int](8)

      for (s <- 0 until 8) {
        val shouldRender = sidesToRender(s)
        val shouldRenderTop = sidesToRender(0)
        val brightness = sideBrightness(s)
        val otherSide = oppositeSide(s)

        var i1 = 0
        val i1Lim = blocks.length
        while (i1 < i1Lim) {
          val lbs = blocks(i1)
          val c = lbs.coords
          val b = lbs.block

          val c2w = c.globalNeighbor(s, chunk.coords)
          val c2 = c2w.getBlockRelChunk
          val crw = c2w.getChunkRelWorld
          val neigh = chunkCache.getChunk(crw)

          if (neigh != null) {
            val bs = neigh.getBlock(c2)

            if (bs.blockType.isTransparent(bs.metadata, otherSide)) {
              brightness(c.value) = neigh.lighting.getBrightness(c2)
              shouldRender.set(c.value)
              sidesCount(s) += 1
              if (s > 1 && b.blockType.isTransparent(b.metadata, s)) {
                shouldRenderTop.set(c.value)
                sidesCount(0) += 1
              } // render the top side
            }
          }

          i1 += 1
        }
      }

      val newBuffers = for (side <- 0 until 8) yield {
        val shouldRender = sidesToRender(side)
        val brightness = sideBrightness(side)
        val buf = BufferUtils.createByteBuffer(sidesCount(side) * ChunkRenderData.blockSideStride(side))

        populateBuffer(blocks, side, shouldRender, brightness, buf)
        buf
      }
      for (side <- 0 until 8) {
        val buf = newBuffers(side)
        buf.flip()
        buffers(side) = buf
      }
    } else if (!chunk.lighting.initialized) {
      chunk.lighting.init(Array.empty)
    }
    renderData = ChunkRenderData(buffers.toIndexedSeq)
    opaqueDeterminer.invalidate()
  }

  private def populateBuffer(blocks: Array[LocalBlockState], side: Int, shouldRender: java.util.BitSet,
                             brightness: Array[Float], buf: ByteBuffer): Unit = {
    val verticesPerInstance = if (side < 2) 6 else 4

    val chunkCoords = chunk.coords

    var i1 = 0
    val i1Lim = blocks.length
    while (i1 < i1Lim) {
      val lbs = blocks(i1)
      val bCoords = lbs.coords
      val block = lbs.block

      if (shouldRender.get(bCoords.value)) {
        val coords = BlockRelWorld.fromChunk(bCoords, chunkCoords)
        buf.putInt(coords.x)
        buf.putInt(coords.y)
        buf.putInt(coords.z)

        val blockType = block.blockType
        buf.putInt(blockType.blockTex(side))
        buf.putFloat(blockType.blockHeight(block.metadata))

        var i2 = 0
        while (i2 < verticesPerInstance) {
          buf.putFloat(brightness(bCoords.value)) // TODO: change in the future to make lighting smoother
          i2 += 1
        }
      }

      i1 += 1
    }
  }

  def appendEntityRenderData(side: Int, append: EntityDataForShader => Unit): Unit = {
    if (chunk.entities.count > 0) {
      val entities = chunk.entities
      val tr = new Matrix4f

      for (ent <- entities.allEntities) {
        val baseT = ent.transform
        val model = ent.model

        val parts = for (part <- model.parts) yield {
          baseT.mul(part.transform, tr)
          val coords4 = tr.transform(new Vector4f(0, 0.5f, 0, 1))
          val coords = CoordUtils.getEnclosingBlock(CylCoords(coords4.x, coords4.y, coords4.z).toBlockCoords)._1
          val cCoords = coords.getChunkRelWorld
          val partChunk = if (cCoords == chunk.coords) Some(chunk) else world.getChunk(cCoords)
          val brightness: Float = partChunk.map(_.lighting.getBrightness(coords.getBlockRelChunk)).getOrElse(0)
          EntityPartDataForShader(
            new Matrix4f(tr),
            part.textureOffset(side),
            part.textureSize(side),
            part.texture(side),
            brightness
          )
        }
        append(EntityDataForShader(model, parts))
      }
    }
  }

  private def oppositeSide(s: Int): Int = {
    if (s < 2) 1 - s else (s - 2 + 3) % 3 + 2
  }

  def unload(): Unit = {
  }
}
