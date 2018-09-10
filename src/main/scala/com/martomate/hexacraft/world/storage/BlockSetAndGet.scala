package com.martomate.hexacraft.world.storage

import com.martomate.hexacraft.block.BlockState
import com.martomate.hexacraft.world.coord.integer.BlockRelWorld

trait BlockSetAndGet {
  def getBlock(coords: BlockRelWorld): BlockState
  def setBlock(coords: BlockRelWorld, block: BlockState): Boolean
  def removeBlock(coords: BlockRelWorld): Boolean
}
