package com.martomate.hexacraft.world.loader

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.coord.fp.BlockCoords
import com.martomate.hexacraft.world.coord.integer.{BlockRelWorld, ChunkRelWorld}

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.collection.mutable

abstract class ChunkLoadingPrioritizerTest extends AnyFlatSpec with Matchers {
  implicit val cylSize: CylinderSize = CylinderSize(4)

  def make(
      origin: PosAndDir = new PosAndDir(),
      distSqFunc: (PosAndDir, ChunkRelWorld) => Double = distSqFuncDefault,
      maxDist: Double = 4
  ): ChunkLoadingPrioritizer

  protected def distSqFuncDefault(p: PosAndDir, c: ChunkRelWorld): Double =
    p.pos.distanceSq(BlockCoords(BlockRelWorld(8, 8, 8, c)).toCylCoords)

  protected def makePos(x: Int, y: Int, z: Int) = PosAndDir(BlockCoords(x, y, z).toCylCoords)

  "nextAddableChunk" should "be the chunk of the origin in the beginning" in {
    make(makePos(0, 0, 0)).nextAddableChunk shouldBe Some(ChunkRelWorld(0, 0, 0))
    make(makePos(17, 0, 0)).nextAddableChunk shouldBe Some(ChunkRelWorld(1, 0, 0))
    make(makePos(-4, 0, 0)).nextAddableChunk shouldBe Some(ChunkRelWorld(-1, 0, 0))
    make(makePos(-4, 160, -30)).nextAddableChunk shouldBe Some(ChunkRelWorld(-1, 10, -2))
  }

  it should "be a neighbor after adding the origin" in {
    val prio = make(makePos(0, 0, 0))
    val coords = ChunkRelWorld(0, 0, 0)
    prio += coords
    coords.neighbors should contain(prio.nextAddableChunk.get)
  }

  it should "be a neighbor of a loaded chunk" in {
    val prio = make()
    val start = ChunkRelWorld(0, 0, 0)
    prio += start
    val chunks = mutable.Set(start)

    for (_ <- 0 until 100) {
      prio.nextAddableChunk.foreach { newChunk =>
        (newChunk, newChunk.neighbors.exists(chunks.contains)) shouldBe ((newChunk, true))

        prio += newChunk
        chunks += newChunk
      }
    }
  }

  it should "not already be loaded" in {
    val prio = make()
    val start = ChunkRelWorld(0, 0, 0)
    prio += start
    val chunks = mutable.Set(start)

    for (_ <- 0 until 100) {
      prio.nextAddableChunk.foreach { newChunk =>
        chunks should not contain newChunk

        prio += newChunk
        chunks += newChunk
      }
    }
  }

  it should "not add forever" in {
    val prio = make()

    val MAX_ADDED = 1000
    var added = 0
    var finished = false
    while (added < MAX_ADDED && !finished) {
      prio.nextAddableChunk match {
        case Some(chunk) =>
          prio += chunk
          added += 1
        case None =>
          finished = true
      }
    }

    finished shouldBe true
  }

  it should "not assume the chunk will be added" in {
    val prio = make()

    prio += prio.nextAddableChunk.get

    val chunk = prio.nextAddableChunk
    prio.nextAddableChunk shouldBe chunk
    prio.nextAddableChunk shouldBe chunk
    prio.nextAddableChunk shouldBe chunk
  }

  "nextRemovableChunk" should "be None in the beginning" in {
    make(makePos(0, 0, 0)).nextRemovableChunk shouldBe None
    make(makePos(17, 0, 0)).nextRemovableChunk shouldBe None
    make(makePos(-4, 0, 0)).nextRemovableChunk shouldBe None
    make(makePos(-4, 160, -30)).nextRemovableChunk shouldBe None
  }

  it should "be a the None after adding the origin" in {
    val prio = make(makePos(0, 0, 0))
    val start = ChunkRelWorld(0, 0, 0)
    prio += start
    prio.nextRemovableChunk shouldBe None
  }

  it should "be None after adding and removing the origin" in {
    val prio = make(makePos(0, 0, 0))
    val start = ChunkRelWorld(0, 0, 0)
    prio += start
    prio -= start
    prio.nextRemovableChunk shouldBe None
  }

  it should "be C after adding the origin and then C far away" in {
    val prio = make(makePos(0, 0, 0))
    val start = ChunkRelWorld(0, 0, 0)
    val far = ChunkRelWorld(10, 0, 0)
    prio += start
    prio += far
    prio.nextRemovableChunk shouldBe Some(far)
  }

  it should "be None after adding and removing C far away" in {
    val prio = make(makePos(0, 0, 0))
    val start = ChunkRelWorld(0, 0, 0)
    val far = ChunkRelWorld(10, 0, 0)
    prio += start
    prio += far
    prio -= far
    prio.nextRemovableChunk shouldBe None
  }

  it should "be the old origin after moving far away" in {
    val origin = makePos(0, 0, 0)
    val prio = make(origin)
    val start = ChunkRelWorld(0, 0, 0)
    prio += start
    origin.pos = makePos(10 * 16, 0, 0).pos
    prio.nextRemovableChunk shouldBe Some(start)
  }

  it should "not assume the chunk will be removed" in {
    val prio = make()

    prio += ChunkRelWorld(100, 0, 0)

    val chunk = prio.nextRemovableChunk
    prio.nextRemovableChunk shouldBe chunk
    prio.nextRemovableChunk shouldBe chunk
    prio.nextRemovableChunk shouldBe chunk
  }
}
