package com.martomate.hexacraft.world.block

import com.martomate.hexacraft.world.coord.integer.BlockRelWorld
import com.martomate.hexacraft.world.storage.BlockSetAndGet

trait BlockBehaviour {
  def onUpdated(coords: BlockRelWorld, world: BlockSetAndGet): Unit
}
