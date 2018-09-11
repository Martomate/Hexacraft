package com.martomate.hexacraft.block

import com.martomate.hexacraft.world.coord.integer.BlockRelWorld
import com.martomate.hexacraft.world.storage.BlockSetAndGet

class BlockBehaviourNothing extends BlockBehaviour {
  def onUpdated(coords: BlockRelWorld, world: BlockSetAndGet): Unit = ()
}
