package hexacraft.world.gen.feature.tree

import hexacraft.world.block.Block
import hexacraft.world.coord.integer.Offset

trait TreeGenStrategy {
  protected final type BlockSpec = (Offset, Block)

  def blocks: Seq[BlockSpec]
}
