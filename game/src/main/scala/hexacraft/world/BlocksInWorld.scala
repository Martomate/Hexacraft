package hexacraft.world

import hexacraft.world.block.BlockState
import hexacraft.world.chunk.{Chunk, ChunkColumnTerrain}
import hexacraft.world.coord.integer.*

trait BlocksInWorld {
  def getColumn(coords: ColumnRelWorld): Option[ChunkColumnTerrain]

  def getChunk(coords: ChunkRelWorld): Option[Chunk]

  def getBlock(coords: BlockRelWorld): BlockState

  def provideColumn(coords: ColumnRelWorld): ChunkColumnTerrain
}
