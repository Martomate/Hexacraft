package com.martomate.hexacraft.world.block

import com.martomate.hexacraft.world.coord.integer.BlockRelWorld
import com.martomate.hexacraft.world.temp.BlockSetAndGet

class BlockBehaviourNothing extends BlockBehaviour {
  def onUpdated(coords: BlockRelWorld, world: BlockSetAndGet): Unit = ()
}
