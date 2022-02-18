package com.martomate.hexacraft.world

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.state.BlockState
import com.martomate.hexacraft.world.chunk.Chunk
import com.martomate.hexacraft.world.column.ChunkColumn
import com.martomate.hexacraft.world.coord.integer._

trait BlocksInWorld {
  def getColumn(coords: ColumnRelWorld): Option[ChunkColumn]

  def getChunk(coords: ChunkRelWorld): Option[Chunk]

  def getBlock(coords: BlockRelWorld): BlockState

  def provideColumn(coords: ColumnRelWorld): ChunkColumn

  def neighbor(side: Int, chunk: Chunk, coords: BlockRelChunk)(implicit cylSize: CylinderSize): (BlockRelChunk, Option[Chunk]) = {
    val off = NeighborOffsets(side)
    val (i2, j2, k2) = (coords.cx + off.dx, coords.cy + off.dy, coords.cz + off.dz)
    val c2 = BlockRelChunk(i2, j2, k2)
    if ((i2 & ~15 | j2 & ~15 | k2 & ~15) == 0) {
      (c2, Some(chunk))
    } else {
      (c2, getChunk(BlockRelWorld(i2, j2, k2, chunk.coords).getChunkRelWorld))
    }
  }

  def neighborChunk(coords: ChunkRelWorld, side: Int)(implicit cylSize: CylinderSize): Option[Chunk] = {
    val off = NeighborOffsets(side)
    getChunk(coords.offset(off))
  }

}
