package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.world.block.state.BlockState
import com.martomate.hexacraft.world.chunk.IChunk
import com.martomate.hexacraft.world.coord.CoordUtils
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.coord.integer.BlockRelWorld
import com.martomate.hexacraft.world.worldlike.IWorld
import org.joml.{Matrix4f, Vector4f}

import scala.collection.mutable

class ChunkRenderer(chunk: IChunk, world: IWorld) {
  import chunk.coords.cylSize.impl

  private var blockRenderers: Option[BlockRendererCollection[BlockRenderer]] = None

  def updateContent(): Unit = {
    onlyKeepBlockRenderersIfChunkNotEmpty()
    if (!chunk.isEmpty) {
      val blocks = chunk.blocks.allBlocks

      if (!chunk.lighting.initialized) {
        chunk.lighting.init(blocks)
      }

      val sidesToRender = Vector.fill(8)(new Array[Boolean](16 * 16 * 16))
      val sideBrightness = Vector.fill(8)(new Array[Float](16 * 16 * 16))
      val sidesCount = new Array[Int](8)
      for ((c, b) <- blocks) {
        for (s <- BlockState.neighborOffsets.indices) {
          val (c2, neigh) = world.neighbor(s, chunk, c)
          val bs = neigh.map(_.getBlock(c2)).getOrElse(BlockState.Air)
          if (bs.blockType.isTransparent(bs.metadata, oppositeSide(s))) {
            sidesToRender(s)(c.value) = true
            sideBrightness(s)(c.value) = neigh.map(_.lighting.getBrightness(c2)).getOrElse(0f)
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
        blockRenderers.get.updateContent(side, sidesCount(side)) {buf =>
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
        }
      }
    } else if (!chunk.lighting.initialized) {
      chunk.lighting.init(Seq.empty)
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
