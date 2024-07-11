package hexacraft.world.gen.tree

import hexacraft.world.block.Block
import hexacraft.world.coord.{NeighborOffsets, Offset}

import scala.collection.mutable
import scala.util.Random

trait PartGenerator {
  protected type BlockSpec = (Offset, Block)

  final def generate(x: Int, y: Int, z: Int, block: Block): Seq[BlockSpec] = {
    generate(Offset(x, y, z), block)
  }

  final def generate(center: Offset, block: Block): Seq[BlockSpec] = {
    generate().map(center + _ -> block)
  }

  def generate(): Seq[Offset]
}

case class PillarGenerator(len: Int) extends PartGenerator {
  override def generate(): Seq[Offset] = {
    for (yy <- 0 until len) yield {
      Offset(0, yy, 0)
    }
  }
}

case class PlatformGenerator(r: Int) extends PartGenerator {
  override def generate(): Seq[Offset] = {
    for {
      dx <- -r to r
      dz <- -r to r
      if math.abs(dx + dz) <= r
    } yield {
      Offset(dx, 0, dz)
    }
  }
}

case class ImperfectPlatformGenerator(rand: Random, r: Int, imperfection: Float) extends PartGenerator {
  override def generate(): Seq[Offset] = {
    val offsets = PlatformGenerator(r).generate()

    offsets.filter(l => l.manhattanDistance < r || rand.nextFloat() > imperfection)
  }
}

/** A blob is randomly grown from the origin into a double cone with adjustable cone heights. The
  * double cone shape comes from the use of manhattan distances. This might be changed in the
  * future. With sufficient irregularity the effect can be greatly reduced.
  * @param numBlocks
  *   the total number of blocks the blob will consist of
  * @param irregularity
  *   how much random the growth speed should be a each simulation step
  * @param flatnessBottom
  *   how much flatter the bottom of the blob should be. 1.0 gives a spherical blob.
  * @param flatnessTop
  *   like `flatnessBottom` but for the top
  */
case class BlobGenerator(
    rand: Random,
    numBlocks: Int,
    irregularity: Float,
    flatnessBottom: Float,
    flatnessTop: Float
) extends PartGenerator {
  override def generate(): Seq[Offset] = {
    val seen = mutable.HashSet.empty[Offset]
    val result = mutable.HashSet.empty[Offset]
    val edge = mutable.PriorityQueue.empty[(Float, Offset)](using Ordering.by(-_._1))
    var time = 0.0f

    val start = Offset(0, 0, 0)
    seen += start
    edge += time -> start

    for _ <- 1 to numBlocks do {
      val top = edge.dequeue()
      time = top._1
      val here = top._2
      result += here

      for off <- NeighborOffsets.all do {
        val pos = here + off
        if !seen(pos) then {
          seen += pos

          val growthResistance =
            if off.dy > 0 then {
              flatnessTop
            } else if off.dy < 0 then {
              flatnessBottom
            } else {
              1.0f
            }

          val newTime = time + (1.0f + (rand.nextFloat() * 2 - 1) * irregularity) * growthResistance
          edge += newTime -> pos
        }
      }
    }
    result.toSeq
  }
}
