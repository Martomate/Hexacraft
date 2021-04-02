package com.martomate.hexacraft.world.gen.feature.tree

import com.martomate.hexacraft.world.block.Block
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.coord.integer.Offset
import org.joml.Vector3d

trait TreeGenStrategy {
  protected type BlockSpec = (Offset, Block)

  def blocks: Seq[BlockSpec]
}
