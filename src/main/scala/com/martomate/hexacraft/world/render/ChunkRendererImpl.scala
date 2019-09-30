package com.martomate.hexacraft.world.render

import java.nio.ByteBuffer

import com.martomate.hexacraft.world.block.state.BlockState
import com.martomate.hexacraft.world.chunk.{ChunkCache, IChunk}
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.coord.integer.{BlockRelChunk, BlockRelWorld, ChunkRelWorld}
import com.martomate.hexacraft.world.coord.{CoordUtils, NeighborOffsets}
import com.martomate.hexacraft.world.worldlike.IWorld
import org.joml.{Matrix4f, Vector4f}
import org.lwjgl.BufferUtils

class ChunkRendererImpl(chunk: IChunk, world: IWorld) extends ChunkRenderer {
  import chunk.coords.cylSize.impl

  def coords: ChunkRelWorld = chunk.coords

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

      val sidesToRender = Array.ofDim[Boolean](8, 16 * 16 * 16)
      val sideBrightness = Array.ofDim[Float](8, 16 * 16 * 16)
      val sidesCount = Array.ofDim[Int](8)

      for (s <- NeighborOffsets.indices) {
        val shouldRender = sidesToRender(s)
        val shouldRenderTop = sidesToRender(0)
        val brightness = sideBrightness(s)
        val otherSide = oppositeSide(s)

        for ((c, b) <- blocks) {
          val (c2, neighOpt) = chunkCache.neighbor(s, chunk, c)

          if (neighOpt.isDefined) {
            val neigh = neighOpt.get

            val bs = neigh.getBlock(c2)

            if (bs.blockType.isTransparent(bs.metadata, otherSide)) {
              brightness(c.value) = neigh.lighting.getBrightness(c2)
              shouldRender(c.value) = true
              sidesCount(s) += 1
              if (s > 1 && b.blockType.isTransparent(b.metadata, s)) {
                shouldRenderTop(c.value) = true
                sidesCount(0) += 1
              } // render the top side
            }
          }
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
      chunk.lighting.init(Seq.empty)
    }
    renderData = ChunkRenderData(buffers)
    opaqueDeterminer.invalidate()
  }

  private def populateBuffer(blocks: Iterable[(BlockRelChunk, BlockState)], side: Int, shouldRender: Array[Boolean],
                             brightness: Array[Float], buf: ByteBuffer): Unit = {
    val verticesPerInstance = if (side < 2) 6 else 4

    for ((bCoords, block) <- blocks) {
      if (shouldRender(bCoords.value)) {
        val coords = BlockRelWorld(bCoords, chunk.coords)
        buf.putInt(coords.x)
        buf.putInt(coords.y)
        buf.putInt(coords.z)

        val blockType = block.blockType
        buf.putInt(blockType.blockTex(side))
        buf.putFloat(blockType.blockHeight(block.metadata))

        for (_ <- 0 until verticesPerInstance) {
          buf.putFloat(brightness(bCoords.value)) // TODO: change in the future to make lighting smoother
        }
      }
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
          val coords = CoordUtils.toBlockCoords(CylCoords(coords4.x, coords4.y, coords4.z).toBlockCoords)._1
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
