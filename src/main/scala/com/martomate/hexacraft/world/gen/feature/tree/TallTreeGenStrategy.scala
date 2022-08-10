package com.martomate.hexacraft.world.gen.feature.tree

import com.martomate.hexacraft.world.block.Block
import com.martomate.hexacraft.world.coord.integer.Offset

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class TallTreeGenStrategy(height: Int, rand: Random)(logBlock: Block, leavesBlock: Block)
    extends TreeGenStrategy {
  override def blocks: Seq[BlockSpec] = {
    val arr = ArrayBuffer.empty[BlockSpec]
    val treeTop = Offset(0, height, 0)
    arr ++= BlobGenerator(rand, 60, 0.05f, 0.7f, 0.7f).generate(treeTop, leavesBlock)
    arr ++= PillarGenerator(height - 1).generate(0, 0, 0, logBlock)
    arr.toSeq
  }
}
