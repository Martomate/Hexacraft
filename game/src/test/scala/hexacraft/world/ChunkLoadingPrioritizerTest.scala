package hexacraft.world

import hexacraft.util.SeqUtils
import hexacraft.world.coord.{BlockCoords, ChunkRelWorld, CylCoords}

import munit.FunSuite

import scala.collection.mutable

class ChunkLoadingPrioritizerTest extends FunSuite {
  given CylinderSize = CylinderSize(4)

  def make(origin: CylCoords = CylCoords(0, 0, 0), maxDist: Double = 4): ChunkLoadingPrioritizer =
    val prio = new ChunkLoadingPrioritizer(maxDist)
    prio.tick(Pose(origin))
    prio

  private def makePos(x: Int, y: Int, z: Int) = BlockCoords(x, y, z).toCylCoords

  test("nextAddableChunk should be the chunk of the origin in the beginning") {
    assertEquals(make(makePos(0, 0, 0)).nextAddableChunk, Some(ChunkRelWorld(0, 0, 0)))
    assertEquals(make(makePos(17, 0, 0)).nextAddableChunk, Some(ChunkRelWorld(1, 0, 0)))
    assertEquals(make(makePos(-4, 0, 0)).nextAddableChunk, Some(ChunkRelWorld(-1, 0, 0)))
    assertEquals(make(makePos(-4, 160, -30)).nextAddableChunk, Some(ChunkRelWorld(-1, 10, -2)))
  }

  test("nextAddableChunk should be a neighbor after adding the origin") {
    val prio = make(makePos(0, 0, 0))
    val coords = ChunkRelWorld(0, 0, 0)
    prio += coords
    assert(coords.neighbors.contains(prio.nextAddableChunk.get))
  }

  test("nextAddableChunk should be a neighbor of a loaded chunk") {
    val prio = make()
    val start = ChunkRelWorld(0, 0, 0)
    prio += start
    val chunks = mutable.Set(start)

    for (_ <- 0 until 100) {
      prio.nextAddableChunk.foreach { newChunk =>
        assertEquals((newChunk, newChunk.neighbors.exists(chunks.contains)), (newChunk, true))

        prio += newChunk
        chunks += newChunk
      }
    }
  }

  test("nextAddableChunk should not already be loaded") {
    val prio = make()
    val start = ChunkRelWorld(0, 0, 0)
    prio += start
    val chunks = mutable.Set(start)

    for (_ <- 0 until 100) {
      prio.nextAddableChunk.foreach { newChunk =>
        assert(!chunks.contains(newChunk))

        prio += newChunk
        chunks += newChunk
      }
    }
  }

  test("nextAddableChunk should not add forever") {
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

    assert(finished)
  }

  test("nextAddableChunk should not assume the chunk will be added") {
    val prio = make()

    prio += prio.nextAddableChunk.get

    val chunk = prio.nextAddableChunk
    assertEquals(prio.nextAddableChunk, chunk)
    assertEquals(prio.nextAddableChunk, chunk)
    assertEquals(prio.nextAddableChunk, chunk)
  }

  test("nextRemovableChunk should be None in the beginning") {
    assertEquals(make(makePos(0, 0, 0)).nextRemovableChunk, None)
    assertEquals(make(makePos(17, 0, 0)).nextRemovableChunk, None)
    assertEquals(make(makePos(-4, 0, 0)).nextRemovableChunk, None)
    assertEquals(make(makePos(-4, 160, -30)).nextRemovableChunk, None)
  }

  test("nextRemovableChunk should be a the None after adding the origin") {
    val prio = make(makePos(0, 0, 0))
    val start = ChunkRelWorld(0, 0, 0)
    prio += start
    assertEquals(prio.nextRemovableChunk, None)
  }

  test("nextRemovableChunk should be None after adding and removing the origin") {
    val prio = make(makePos(0, 0, 0))
    val start = ChunkRelWorld(0, 0, 0)
    prio += start
    prio -= start
    assertEquals(prio.nextRemovableChunk, None)
  }

  test("nextRemovableChunk should be C after adding the origin and then C far away") {
    val prio = make(makePos(0, 0, 0))
    val start = ChunkRelWorld(0, 0, 0)
    val far = ChunkRelWorld(10, 0, 0)
    prio += start
    prio += far
    assertEquals(prio.nextRemovableChunk, Some(far))
  }

  test("nextRemovableChunk should be None after adding and removing C far away") {
    val prio = make(makePos(0, 0, 0))
    val start = ChunkRelWorld(0, 0, 0)
    val far = ChunkRelWorld(10, 0, 0)
    prio += start
    prio += far
    prio -= far
    assertEquals(prio.nextRemovableChunk, None)
  }

  test("nextRemovableChunk should be the old origin after moving far away") {
    val origin = makePos(0, 0, 0)
    val prio = make(origin)
    val start = ChunkRelWorld(0, 0, 0)
    prio += start
    prio.tick(Pose(makePos(10 * 16, 0, 0)))
    assertEquals(prio.nextRemovableChunk, Some(start))
  }

  test("nextRemovableChunk should not assume the chunk will be removed") {
    val prio = make()

    prio += ChunkRelWorld(100, 0, 0)

    val chunk = prio.nextRemovableChunk
    assertEquals(prio.nextRemovableChunk, chunk)
    assertEquals(prio.nextRemovableChunk, chunk)
    assertEquals(prio.nextRemovableChunk, chunk)
  }

  test("nextAddableChunk should return chunks in order of increasing distance after adding the first one") {
    val origin = makePos(0, 0, 0)
    val prio = make(origin)
    prio += prio.nextAddableChunk.get
    var prevDistSq: Double = 0
    SeqUtils.whileSome(10000, prio.nextAddableChunk) { chunk =>
      val distSq = ChunkLoadingPrioritizer.distSq(Pose(origin), chunk)
      assert(distSq >= prevDistSq)
      prevDistSq = distSq
      prio += chunk
    }
  }

  test("nextAddableChunk should not skip chunks, creating holes") {
    val origin = makePos(0, 0, 0)
    val maxDist = 10
    val maxDistSqInBlocks = math.pow(maxDist * 16, 2)
    val prio = make(origin, maxDist = maxDist)
    val chunks: mutable.Set[ChunkRelWorld] =
      mutable.HashSet.from(
        ChunkRelWorld(0, 0, 0)
          .extendedNeighbors(4)
          .toSeq
          .filter(c => ChunkLoadingPrioritizer.distSq(Pose(origin), c) <= maxDistSqInBlocks)
      )
    SeqUtils.whileSome(10000, prio.nextAddableChunk) { chunk =>
      prio += chunk
      chunks -= chunk
    }
    assert(chunks.isEmpty)
  }

  test("nextAddableChunk should retain order when origin is moved") {
    val origin = makePos(0, 0, 0)
    val prio = make(origin)
    prio += prio.nextAddableChunk.get

    assertEquals(nextAddableChunkWPos(32, 8, 8), Some(ChunkRelWorld(1, 0, 0)))
    assertEquals(nextAddableChunkWPos(-32, 8, 8), Some(ChunkRelWorld(-1, 0, 0)))
    assertEquals(nextAddableChunkWPos(8, 32, 8), Some(ChunkRelWorld(0, 1, 0)))
    assertEquals(nextAddableChunkWPos(8, -32, 8), Some(ChunkRelWorld(0, -1, 0)))
    assertEquals(nextAddableChunkWPos(8, 8, 32), Some(ChunkRelWorld(0, 0, 1)))
    assertEquals(nextAddableChunkWPos(8, 8, -32), Some(ChunkRelWorld(0, 0, -1)))

    def nextAddableChunkWPos(x: Int, y: Int, z: Int): Option[ChunkRelWorld] = {
      prio.tick(Pose(BlockCoords(x, y, z).toCylCoords))
      prio.reorderPQs()
      prio.nextAddableChunk
    }
  }

  test("nextAddableChunk should retain order when origin is rotated".ignore) {}

  test("nextRemovableChunk should return chunks in farthest-first order") {
    val origin = makePos(0, 0, 0)
    val prio = make(origin)
    for (_ <- 1 to 100) prio.nextAddableChunk.foreach(prio += _)

    var prevDistSq: Double = Double.MaxValue
    SeqUtils.whileSome(100, prio.nextRemovableChunk) { chunk =>
      val distSq = ChunkLoadingPrioritizer.distSq(Pose(origin), chunk)
      assert(distSq <= prevDistSq)
      prevDistSq = distSq
      prio -= chunk
    }

    assertEquals(prio.nextRemovableChunk, None)
  }

  test("nextRemovableChunk should retain order when origin is moved") {
    val origin = makePos(0, 0, 0)
    val prio = make(origin, maxDist = 1)
    prio += ChunkRelWorld(0, 0, 0)
    prio += ChunkRelWorld(1, 0, 0)
    prio += ChunkRelWorld(0, 1, 0)
    prio += ChunkRelWorld(0, 0, 1)
    prio += ChunkRelWorld(-1, 0, 0)
    prio += ChunkRelWorld(0, -1, 0)
    prio += ChunkRelWorld(0, 0, -1)

    assertEquals(nextRemovableChunkWPos(32, 8, 8), Some(ChunkRelWorld(-1, 0, 0)))
    assertEquals(nextRemovableChunkWPos(-32, 8, 8), Some(ChunkRelWorld(1, 0, 0)))
    assertEquals(nextRemovableChunkWPos(8, 32, 8), Some(ChunkRelWorld(0, -1, 0)))
    assertEquals(nextRemovableChunkWPos(8, -32, 8), Some(ChunkRelWorld(0, 1, 0)))
    assertEquals(nextRemovableChunkWPos(8, 8, 32), Some(ChunkRelWorld(0, 0, -1)))
    assertEquals(nextRemovableChunkWPos(8, 8, -32), Some(ChunkRelWorld(0, 0, 1)))

    def nextRemovableChunkWPos(x: Int, y: Int, z: Int): Option[ChunkRelWorld] = {
      prio.tick(Pose(BlockCoords(x, y, z).toCylCoords))
      prio.reorderPQs()
      prio.nextRemovableChunk
    }
  }

  test("nextRemovableChunk should retain order when origin is rotated".ignore) {}
}
