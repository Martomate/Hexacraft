package hexacraft.client.render

import hexacraft.shaders.BlockShader
import hexacraft.util.Loop
import hexacraft.world.{BlocksInWorld, CylinderSize}
import hexacraft.world.chunk.LocalBlockState
import hexacraft.world.coord.ChunkRelWorld

import org.lwjgl.BufferUtils

import java.nio.ByteBuffer

class ChunkRenderData(
    val opaqueBlocks: Option[IndexedSeq[ByteBuffer]],
    val transmissiveBlocks: Option[IndexedSeq[ByteBuffer]]
)

object ChunkRenderData {
  def empty: ChunkRenderData = new ChunkRenderData(None, None)

  def apply(
      coords: ChunkRelWorld,
      world: BlocksInWorld,
      blockTextureIndices: Map[String, IndexedSeq[Int]]
  )(using CylinderSize): ChunkRenderData = {
    new ChunkRenderData(
      Some(makeBlockVboData(coords, world, false, blockTextureIndices)),
      Some(makeBlockVboData(coords, world, true, blockTextureIndices))
    )
  }

  private def makeBlockVboData(
      chunkCoords: ChunkRelWorld,
      world: BlocksInWorld,
      transmissiveBlocks: Boolean,
      blockTextureIndices: Map[String, IndexedSeq[Int]]
  )(using CylinderSize): IndexedSeq[ByteBuffer] = {
    val builder = BlockVertexDataBuilder.fromChunk(chunkCoords, world, transmissiveBlocks, blockTextureIndices)

    val blocksBuffers = Array.ofDim[ByteBuffer](8)

    Loop.rangeUntil(0, 8) { side =>
      val vertices = builder.calculateVertices(side)

      val bytesPerBlock = BlockShader.bytesPerVertex(side) * BlockShader.verticesPerBlock(side)
      val buf = BufferUtils.createByteBuffer(vertices.size * bytesPerBlock)

      Loop.array(vertices) { v =>
        v.fill(buf)
      }

      buf.flip()
      blocksBuffers(side) = buf
    }

    blocksBuffers.toIndexedSeq
  }
}
