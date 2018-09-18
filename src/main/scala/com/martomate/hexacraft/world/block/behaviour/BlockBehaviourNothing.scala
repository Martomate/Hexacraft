package com.martomate.hexacraft.world.block.behaviour

import com.martomate.hexacraft.world.coord.integer.BlockRelWorld
import com.martomate.hexacraft.world.block.setget.BlockSetAndGet

class BlockBehaviourNothing extends BlockBehaviour {
  def onUpdated(coords: BlockRelWorld, world: BlockSetAndGet): Unit = ()
}
