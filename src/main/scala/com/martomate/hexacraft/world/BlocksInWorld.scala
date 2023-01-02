package com.martomate.hexacraft.world

import com.martomate.hexacraft.world.block.BlockState
import com.martomate.hexacraft.world.chunk.{Chunk, ChunkColumn, ChunkColumnTerrain}
import com.martomate.hexacraft.world.coord.integer.*

trait BlocksInWorld {
  def getColumn(coords: ColumnRelWorld): Option[ChunkColumnTerrain]

  def getChunk(coords: ChunkRelWorld): Option[Chunk]

  def getBlock(coords: BlockRelWorld): BlockState

  def provideColumn(coords: ColumnRelWorld): ChunkColumnTerrain
}
