package com.martomate.hexacraft.world

import com.martomate.hexacraft.world.block.state.BlockState
import com.martomate.hexacraft.world.chunk.{Chunk, ChunkColumn}
import com.martomate.hexacraft.world.coord.integer._

trait BlocksInWorld {
  def getColumn(coords: ColumnRelWorld): Option[ChunkColumn]

  def getChunk(coords: ChunkRelWorld): Option[Chunk]

  def getBlock(coords: BlockRelWorld): BlockState

  def provideColumn(coords: ColumnRelWorld): ChunkColumn
}
