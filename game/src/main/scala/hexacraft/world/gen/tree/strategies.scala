package hexacraft.world.gen.tree

import hexacraft.world.block.Block
import hexacraft.world.coord.integer.{NeighborOffsets, Offset}

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

trait TreeGenStrategy {
  protected final type BlockSpec = (Offset, Block)

  def blocks: Seq[BlockSpec]
}

class HugeTreeGenStrategy(size: Int, stems: Int, rand: Random) extends TreeGenStrategy {
  override def blocks: Seq[(Offset, Block)] = {
    val builder = new Builder

    for (_ <- 0 until stems) {
      builder.generateStem(Offset(0, 0, 0), 7, 3, 40)
      builder.generateTreeCrown(Offset(0, 40, 0), 60, 0.7f, 0.5f, 0.5f)
    }

    builder.blocks
  }

  private class Builder {
    val logs: ArrayBuffer[BlockSpec] = ArrayBuffer.empty
    val leaves: ArrayBuffer[BlockSpec] = ArrayBuffer.empty

    def generateStem(
        start: Offset,
        crossSectionBottom: Int,
        crossSectionTop: Int,
        length: Int
    ): Unit = {
      require(crossSectionBottom <= 7, "Trees with cross section greater than 7 are not supported")
      require(
        crossSectionBottom >= crossSectionTop,
        "Trees with a wider top than bottom are not supported"
      )

      val firstNeighborIndex = rand.nextInt(6)
      val neighbors = NeighborOffsets.all.filter(_.dy == 0).map(start + _)
      val reorderedNeighbors =
        rand.shuffle(neighbors.drop(firstNeighborIndex) ++ neighbors.take(firstNeighborIndex))
      val neighborRoots = reorderedNeighbors.slice(crossSectionTop - 1, crossSectionBottom - 1)
      val neighborHeights =
        randomlyDivideInterval(crossSectionBottom - crossSectionTop + 1, 0.1f, length).map(_.round)

      logs ++= PillarGenerator(length).generate(start, Block.Log)
      for (i <- 0 until crossSectionTop - 1)
        logs ++= PillarGenerator(length).generate(reorderedNeighbors(i), Block.Log)

      for ((r, h) <- neighborRoots.zip(neighborHeights))
        logs ++= PillarGenerator(h).generate(r, Block.Log)
    }

    private def randomlyDivideInterval(parts: Int, randomness: Float, length: Int): Seq[Float] = {
      val dists = for (_ <- 1 to parts) yield 1.0f + randomness * (rand.nextFloat() * 2 - 1)
      val acc = dists.scanLeft(0.0f)(_ + _).drop(1)
      val totalDist = acc.last
      acc.map(_ / totalDist * length).dropRight(1)
    }

    def generateTreeCrown(
        center: Offset,
        size: Float,
        irregularity: Float,
        flatnessBottom: Float,
        flatnessTop: Float
    ): Unit = {
      val blobGen =
        BlobGenerator(rand, (size * 40 + 1).toInt, irregularity, flatnessBottom, flatnessTop)
      leaves ++= blobGen.generate(center, Block.Leaves)
    }

    def blocks: Seq[BlockSpec] = (leaves ++ logs).toSeq
  }

}

class TallTreeGenStrategy(height: Int, rand: Random)(logBlock: Block, leavesBlock: Block) extends TreeGenStrategy {
  override def blocks: Seq[BlockSpec] = {
    val arr = ArrayBuffer.empty[BlockSpec]
    val treeTop = Offset(0, height, 0)
    arr ++= BlobGenerator(rand, 60, 0.05f, 0.7f, 0.7f).generate(treeTop, leavesBlock)
    arr ++= PillarGenerator(height - 1).generate(0, 0, 0, logBlock)
    arr.toSeq
  }
}

class ShortTreeGenStrategy(logBlock: Block, leavesBlock: Block) extends TreeGenStrategy {
  override def blocks: Seq[BlockSpec] =
    PlatformGenerator(1).generate(0, 6, 0, leavesBlock) ++
      PlatformGenerator(2).generate(0, 7, 0, leavesBlock) ++
      PlatformGenerator(1).generate(0, 8, 0, leavesBlock) ++
      PlatformGenerator(0).generate(0, 9, 0, leavesBlock) ++
      PillarGenerator(9).generate(0, 0, 0, logBlock)
}
