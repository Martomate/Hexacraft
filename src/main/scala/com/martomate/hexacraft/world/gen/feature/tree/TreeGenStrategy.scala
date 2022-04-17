package com.martomate.hexacraft.world.gen.feature.tree

import com.martomate.hexacraft.world.block.Block
import com.martomate.hexacraft.world.coord.integer.Offset

trait TreeGenStrategy {
  protected type BlockSpec = (Offset, Block)

  def blocks: Seq[BlockSpec]
}
