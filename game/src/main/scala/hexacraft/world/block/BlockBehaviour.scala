package hexacraft.world.block

import hexacraft.world.CylinderSize
import hexacraft.world.coord.BlockRelWorld

trait BlockBehaviour {
  def onUpdated(coords: BlockRelWorld, block: Block, world: BlockRepository)(using CylinderSize): Unit
}
