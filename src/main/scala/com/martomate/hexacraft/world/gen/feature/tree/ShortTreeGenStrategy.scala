package com.martomate.hexacraft.world.gen.feature.tree

import com.martomate.hexacraft.world.block.Blocks

class ShortTreeGenStrategy extends TreeGenStrategy {
  override def blocks: Seq[BlockSpec] =
    PlatformGenerator(1).generate(0, 6, 0, Blocks.Leaves) ++
      PlatformGenerator(2).generate(0, 7, 0, Blocks.Leaves) ++
      PlatformGenerator(1).generate(0, 8, 0, Blocks.Leaves) ++
      PlatformGenerator(0).generate(0, 9, 0, Blocks.Leaves) ++
      PillarGenerator(9).generate(0, 0, 0, Blocks.Log)
}
