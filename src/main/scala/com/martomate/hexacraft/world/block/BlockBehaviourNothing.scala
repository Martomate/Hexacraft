package com.martomate.hexacraft.world.block

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.{BlockBehaviour, BlockSetAndGet}
import com.martomate.hexacraft.world.coord.integer.BlockRelWorld

class BlockBehaviourNothing extends BlockBehaviour {
  def onUpdated(coords: BlockRelWorld, world: BlockSetAndGet)(using CylinderSize, Blocks): Unit = ()
}
