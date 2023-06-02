package com.martomate.hexacraft.world.block

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.coord.integer.BlockRelWorld

trait BlockBehaviour {
  def onUpdated(coords: BlockRelWorld, block: Block, world: BlockRepository)(using CylinderSize, Blocks): Unit
}
