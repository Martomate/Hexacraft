package hexacraft.world.render

import hexacraft.world.{BlocksInWorld, CylinderSize}
import hexacraft.world.chunk.LocalBlockState
import hexacraft.world.coord.ChunkRelWorld

import java.nio.ByteBuffer

class ChunkRenderData(
    val opaqueBlocks: Option[IndexedSeq[ByteBuffer]],
    val transmissiveBlocks: Option[IndexedSeq[ByteBuffer]]
)

object ChunkRenderData {
  def empty: ChunkRenderData = new ChunkRenderData(None, None)

  def apply(
      coords: ChunkRelWorld,
      blocks: Array[LocalBlockState],
      world: BlocksInWorld,
      blockTextureIndices: Map[String, IndexedSeq[Int]]
  )(using CylinderSize): ChunkRenderData = {
    if blocks.isEmpty then {
      return new ChunkRenderData(None, None)
    }

    new ChunkRenderData(
      Some(BlockVboData.fromChunk(coords, blocks, world, false, blockTextureIndices)),
      Some(BlockVboData.fromChunk(coords, blocks, world, true, blockTextureIndices))
    )
  }
}
