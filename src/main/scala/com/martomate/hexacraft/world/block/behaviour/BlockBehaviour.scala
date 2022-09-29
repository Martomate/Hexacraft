package com.martomate.hexacraft.world.block.behaviour

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.BlockSetAndGet
import com.martomate.hexacraft.world.coord.integer.BlockRelWorld

trait BlockBehaviour {
  def onUpdated(coords: BlockRelWorld, world: BlockSetAndGet)(implicit cylSize: CylinderSize): Unit
}
