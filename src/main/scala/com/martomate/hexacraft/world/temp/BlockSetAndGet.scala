package com.martomate.hexacraft.world.temp

import com.martomate.hexacraft.world.block.BlockState
import com.martomate.hexacraft.world.coord.integer.BlockRelWorld

trait BlockSetAndGet {
  def getBlock(coords: BlockRelWorld): BlockState
  def setBlock(coords: BlockRelWorld, block: BlockState): Boolean
  def removeBlock(coords: BlockRelWorld): Boolean
}
