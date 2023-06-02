package com.martomate.hexacraft.world.block

import com.martomate.hexacraft.world.coord.integer.BlockRelWorld

trait BlockRepository {
  def getBlock(coords: BlockRelWorld): BlockState
  def setBlock(coords: BlockRelWorld, block: BlockState): Unit
  def removeBlock(coords: BlockRelWorld): Unit
}
