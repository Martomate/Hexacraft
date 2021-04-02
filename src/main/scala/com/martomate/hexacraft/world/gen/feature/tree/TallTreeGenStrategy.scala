package com.martomate.hexacraft.world.gen.feature.tree

import com.martomate.hexacraft.world.block.Blocks
import com.martomate.hexacraft.world.coord.integer.Offset

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class TallTreeGenStrategy(height: Int, rand: Random) extends TreeGenStrategy {
  override def blocks: Seq[BlockSpec] = {
    val arr = ArrayBuffer.empty[BlockSpec]
    val treeTop = Offset(0, height, 0)
    arr ++= BlobGenerator(rand, 60, 0.05f, 0.7f, 0.7f).generate(treeTop, Blocks.Leaves)
    /*
    arr ++= makeImperfectPlatform(0, height-6, 0, 1, rand, 0.1f, Blocks.Leaves)
    arr ++= makePlatform(0, height-5, 0, 1, Blocks.Leaves)
    arr ++= makeImperfectPlatform(0, height-4, 0, 2, rand, 0.1f, Blocks.Leaves)
    arr ++= makeImperfectPlatform(0, height-3, 0, 2, rand, 0.1f, Blocks.Leaves)
    arr ++= makePlatform(0, height-2, 0, 1, Blocks.Leaves)
    arr ++= makeImperfectPlatform(0, height-1, 0, 1, rand, 0.1f, Blocks.Leaves)
    arr ++= makePlatform(0, height-0, 0, 0, Blocks.Leaves)
    */
    arr ++= PillarGenerator(height - 1).generate(0, 0, 0, Blocks.Log)
    arr.toSeq
  }
}
