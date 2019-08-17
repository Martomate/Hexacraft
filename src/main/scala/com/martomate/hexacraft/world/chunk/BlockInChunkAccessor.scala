package com.martomate.hexacraft.world.chunk

import com.martomate.hexacraft.world.block.state.BlockState
import com.martomate.hexacraft.world.coord.integer.BlockRelChunk

trait BlockInChunkAccessor {
  def getBlock(coords: BlockRelChunk): BlockState
  def setBlock(blockCoords: BlockRelChunk, block: BlockState): Unit
  def removeBlock(coords: BlockRelChunk): Unit
}
