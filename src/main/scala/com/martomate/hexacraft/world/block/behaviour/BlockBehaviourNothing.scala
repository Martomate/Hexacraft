package com.martomate.hexacraft.world.block.behaviour

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.setget.BlockSetAndGet
import com.martomate.hexacraft.world.coord.integer.BlockRelWorld

class BlockBehaviourNothing extends BlockBehaviour {
  def onUpdated(coords: BlockRelWorld, world: BlockSetAndGet)(implicit cylSize: CylinderSize): Unit = ()
}
