package com.martomate.hexacraft.world.storage

import com.martomate.hexacraft.block.BlockState
import com.martomate.hexacraft.world.coord.integer.BlockRelWorld

trait ChunkBlockListener {
  def onSetBlock(coords: BlockRelWorld, prev: BlockState, now: BlockState): Unit
}
