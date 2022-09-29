package com.martomate.hexacraft.world.gen.feature.tree

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.{Block, BlockState}
import com.martomate.hexacraft.world.coord.integer.{BlockRelWorld, Offset}
import com.martomate.hexacraft.world.gen.PlannedWorldChange

class GenTree(at: BlockRelWorld, strategy: TreeGenStrategy)(implicit cylSize: CylinderSize) {
  def generate(): PlannedWorldChange = {
    val worldChange = new PlannedWorldChange
    for ((c, b) <- strategy.blocks) {
      setBlockAt(at, c, b)(worldChange)
    }
    worldChange
  }

  private def setBlockAt(at: BlockRelWorld, c: Offset, b: Block)(
      world: PlannedWorldChange
  ): Unit = {
    val ch = at.offset(c)
    val bs = new BlockState(b)
    world.setBlock(ch, bs)
  }
}
