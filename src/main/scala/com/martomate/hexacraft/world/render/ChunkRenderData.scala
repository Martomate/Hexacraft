package com.martomate.hexacraft.world.render

import java.nio.ByteBuffer

object ChunkRenderData {
  def blockSideStride(side: Int): Int = if (side < 2) (5 + 6) * 4 else (5 + 4) * 4
}

case class ChunkRenderData(blockSide: IndexedSeq[ByteBuffer])
