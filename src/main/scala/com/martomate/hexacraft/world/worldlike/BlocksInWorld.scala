package com.martomate.hexacraft.world.worldlike

import com.martomate.hexacraft.world.block.state.BlockState
import com.martomate.hexacraft.world.chunk.IChunk
import com.martomate.hexacraft.world.coord.integer.{BlockRelChunk, BlockRelWorld, ChunkRelWorld, ColumnRelWorld}
import com.martomate.hexacraft.world.column.ChunkColumn

trait BlocksInWorld {
  def getColumn(coords: ColumnRelWorld): Option[ChunkColumn]
  def getChunk(coords: ChunkRelWorld): Option[IChunk]
  def getBlock(coords: BlockRelWorld): BlockState

  def provideColumn(coords: ColumnRelWorld): ChunkColumn

  def neighbor(side: Int, chunk: IChunk, coords: BlockRelChunk): (BlockRelChunk, Option[IChunk]) = {
    val (i, j, k) = BlockState.neighborOffsets(side)
    val (i2, j2, k2) = (coords.cx + i, coords.cy + j, coords.cz + k)
    val c2 = BlockRelChunk(i2, j2, k2)(coords.cylSize)
    if ((i2 & ~15 | j2 & ~15 | k2 & ~15) == 0) {
      (c2, Some(chunk))
    } else {
      (c2, getChunk(BlockRelWorld(i2, j2, k2, chunk.coords).getChunkRelWorld))
    }
  }

  def neighborChunk(coords: ChunkRelWorld, side: Int): Option[IChunk] = {
    val (dx, dy, dz) = ChunkRelWorld.neighborOffsets(side)
    getChunk(coords.offset(dx, dy, dz))
  }

  def neighborChunks(coords: ChunkRelWorld): Iterable[IChunk] = Iterable.tabulate(8)(i => neighborChunk(coords, i)).flatten
}
