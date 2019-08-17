package com.martomate.hexacraft.world.block.setget

import com.martomate.hexacraft.world.block.state.BlockState
import com.martomate.hexacraft.world.coord.integer.BlockRelWorld

trait BlockSetAndGet {
  def getBlock(coords: BlockRelWorld): BlockState
  def setBlock(coords: BlockRelWorld, block: BlockState): Unit
  def removeBlock(coords: BlockRelWorld): Unit
}
