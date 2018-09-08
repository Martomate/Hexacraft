package com.martomate.hexacraft.world.storage

import com.martomate.hexacraft.block.BlockState
import com.martomate.hexacraft.world.coord.{BlockRelChunk, BlockRelWorld, ChunkRelWorld, ColumnRelWorld}

trait BlocksInWorld {
  def getColumn(coords: ColumnRelWorld): Option[ChunkColumn]
  def getChunk(coords: ChunkRelWorld): Option[IChunk]
  def getBlock(coords: BlockRelWorld): BlockState

  def neighbor(side: Int, chunk: IChunk, coords: BlockRelChunk): (BlockRelChunk, Option[IChunk]) = {
    val (i, j, k) = BlockState.neighborOffsets(side)
    val (i2, j2, k2) = (coords.cx + i, coords.cy + j, coords.cz + k)
    val c2 = BlockRelChunk(i2, j2, k2, coords.cylSize)
    if ((i2 & ~15 | j2 & ~15 | k2 & ~15) == 0) {
      (c2, Some(chunk))
    } else {
      (c2, getChunk(chunk.coords.withBlockCoords(i2, j2, k2).getChunkRelWorld))
    }
  }

  def neighborChunk(coords: ChunkRelWorld, side: Int): Option[IChunk] = {
    val (dx, dy, dz) = ChunkRelWorld.neighborOffsets(side)
    getChunk(coords.offset(dx, dy, dz))
  }

  def neighborChunks(coords: ChunkRelWorld): Iterable[IChunk] = Iterable.tabulate(8)(i => neighborChunk(coords, i)).flatten
}
