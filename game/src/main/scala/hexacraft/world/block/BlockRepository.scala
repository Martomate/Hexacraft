package hexacraft.world.block

import hexacraft.world.coord.BlockRelWorld

trait BlockRepository {
  def getBlock(coords: BlockRelWorld): BlockState
  def setBlock(coords: BlockRelWorld, block: BlockState): Unit
  def removeBlock(coords: BlockRelWorld): Unit
}
