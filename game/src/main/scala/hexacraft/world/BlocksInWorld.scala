package hexacraft.world

import hexacraft.world.block.BlockState
import hexacraft.world.chunk.{Chunk, ChunkColumnHeightMap}
import hexacraft.world.coord.{BlockRelWorld, ChunkRelWorld, ColumnRelWorld}

trait BlocksInWorld {
  def getColumn(coords: ColumnRelWorld): Option[ChunkColumnHeightMap]

  def getChunk(coords: ChunkRelWorld): Option[Chunk]

  def getBlock(coords: BlockRelWorld): BlockState
}
