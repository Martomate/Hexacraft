package com.martomate.hexacraft.world.render

import java.nio.ByteBuffer

object ChunkRenderData {
  def blockSideStride(side: Int): Int = if (side < 2) (5 + 7) * 4 else (5 + 4) * 4

  def empty: ChunkRenderData = new ChunkRenderData(IndexedSeq.fill(8)(null))
}

case class ChunkRenderData(blockSide: IndexedSeq[ByteBuffer])
