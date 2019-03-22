package com.martomate.hexacraft.world.render

import java.nio.ByteBuffer

import com.martomate.hexacraft.world.block.state.BlockState
import com.martomate.hexacraft.world.chunk.IChunk
import com.martomate.hexacraft.world.coord.CoordUtils
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.coord.integer.{BlockRelWorld, ChunkRelWorld}
import com.martomate.hexacraft.world.worldlike.{ChunkCache, IWorld}
import org.joml.{Matrix4f, Vector4f}
import org.lwjgl.BufferUtils

case class ChunkRenderData(blockSide: IndexedSeq[ByteBuffer])

object ChunkRenderData {
  def blockSideStride(side: Int): Int = if (side < 2) (5 + 6) * 4 else (5 + 4) * 4
}

trait ChunkRenderer {
  def coords: ChunkRelWorld

  def canGetToSide(fromSide: Int, toSide: Int): Boolean

  def getRenderData: ChunkRenderData

  def updateContent(): Unit

  def appendEntityRenderData(side: Int, append: EntityDataForShader => Unit): Unit

  def unload(): Unit
}

class ChunkRendererImpl(chunk: IChunk, world: IWorld) extends ChunkRenderer {
  import chunk.coords.cylSize.impl

  def coords: ChunkRelWorld = chunk.coords

  private val opaqueDeterminer: ChunkOpaqueDeterminer = new ChunkOpaqueDeterminerSimple(chunk.coords, chunk)

  def canGetToSide(fromSide: Int, toSide: Int): Boolean = opaqueDeterminer.canGetToSide(fromSide, toSide)

  private var renderData: ChunkRenderData = _
  def getRenderData: ChunkRenderData = renderData

  def updateContent(): Unit = {
    val buffers: Array[ByteBuffer] = new Array(8)

    if (!chunk.hasNoBlocks) {
      val blocks = chunk.blocks.allBlocks

      if (!chunk.lighting.initialized) {
        chunk.lighting.init(blocks)
      }

      val blockStates = blocks.toMap

      val chunkCache = new ChunkCache(world)

      val sidesToRender = Vector.fill(8)(new Array[Boolean](16 * 16 * 16))
      val sideBrightness = Vector.fill(8)(new Array[Float](16 * 16 * 16))
      val sidesCount = new Array[Int](8)
      for (s <- BlockState.neighborOffsets.indices) {
        for ((c, b) <- blocks) {
          val (c2, neigh) = chunkCache.neighbor(s, chunk, c)

          val bs = if (neigh.contains(chunk)) blockStates.getOrElse(c2, BlockState.Air) else neigh.map(_.getBlock(c2)).getOrElse(BlockState.Air)

          if (neigh.isDefined && bs.blockType.isTransparent(bs.metadata, oppositeSide(s))) {
            sidesToRender(s)(c.value) = true
            sideBrightness(s)(c.value) = neigh.get.lighting.getBrightness(c2)
            sidesCount(s) += 1
            if (s > 1 && b.blockType.isTransparent(b.metadata, s)) {
              sidesToRender(0)(c.value) = true
              sidesCount(0) += 1
            } // render the top side
          }
        }
      }

      for (side <- 0 until 8) {
        val verticesPerInstance = if (side < 2) 6 else 4
        val buf = BufferUtils.createByteBuffer(sidesCount(side) * ChunkRenderData.blockSideStride(side))
          for ((bCoords, block) <- blocks) {
            val coords = BlockRelWorld(bCoords, chunk.coords)
            if (sidesToRender(side)(bCoords.value)) {
              buf.putInt(coords.x)
              buf.putInt(coords.y)
              buf.putInt(coords.z)
              val blockType = block.blockType
              buf.putInt(blockType.blockTex(side))
              buf.putFloat(blockType.blockHeight(block.metadata))
              for (i <- 0 until verticesPerInstance) {
                buf.putFloat(sideBrightness(side)(bCoords.value))// TODO: change in the future to make lighting smoother
              }
            }
          }
          buffers(side) = buf
      }
      for (side <- 0 until 8) {
        val b = buffers(side)
        b.flip()
      }
    } else if (!chunk.lighting.initialized) {
      chunk.lighting.init(Seq.empty)
    }
    renderData = ChunkRenderData(buffers)
    opaqueDeterminer.invalidate()
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
