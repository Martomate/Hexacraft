package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.world.chunk.storage.ChunkStorage

class ChunkOpaqueDeterminerSimpleTest extends ChunkOpaqueDeterminerTest {
  def make(chunk: ChunkStorage): ChunkOpaqueDeterminer = new ChunkOpaqueDeterminerSimple(chunk.chunkCoords, chunk)
}
