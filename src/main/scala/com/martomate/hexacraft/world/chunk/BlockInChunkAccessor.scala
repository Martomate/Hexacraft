package com.martomate.hexacraft.world.chunk

import com.martomate.hexacraft.world.block.{Block, BlockState}
import com.martomate.hexacraft.world.coord.integer.BlockRelChunk

trait BlockInChunkAccessor:
  def getBlock(coords: BlockRelChunk): BlockState
