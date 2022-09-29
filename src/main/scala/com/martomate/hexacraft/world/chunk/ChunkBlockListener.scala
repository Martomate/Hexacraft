package com.martomate.hexacraft.world.chunk

import com.martomate.hexacraft.world.block.BlockState
import com.martomate.hexacraft.world.coord.integer.BlockRelWorld

trait ChunkBlockListener {
  def onSetBlock(coords: BlockRelWorld, prev: BlockState, now: BlockState): Unit
}
