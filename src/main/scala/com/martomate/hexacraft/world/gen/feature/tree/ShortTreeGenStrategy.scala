package com.martomate.hexacraft.world.gen.feature.tree

import com.martomate.hexacraft.world.block.Block

class ShortTreeGenStrategy(logBlock: Block, leavesBlock: Block) extends TreeGenStrategy {
  override def blocks: Seq[BlockSpec] =
    PlatformGenerator(1).generate(0, 6, 0, leavesBlock) ++
      PlatformGenerator(2).generate(0, 7, 0, leavesBlock) ++
      PlatformGenerator(1).generate(0, 8, 0, leavesBlock) ++
      PlatformGenerator(0).generate(0, 9, 0, leavesBlock) ++
      PillarGenerator(9).generate(0, 0, 0, logBlock)
}
