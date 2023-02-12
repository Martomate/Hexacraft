package com.martomate.hexacraft.world.loader

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.coord.fp.BlockCoords
import com.martomate.hexacraft.world.coord.integer.{BlockRelWorld, ChunkRelWorld}

import munit.FunSuite
import scala.collection.mutable

abstract class ChunkLoadingPrioritizerTest extends FunSuite {
  implicit val cylSize: CylinderSize = CylinderSize(4)

  def make(
      origin: PosAndDir = new PosAndDir(),
      distSqFunc: (PosAndDir, ChunkRelWorld) => Double = distSqFuncDefault,
      maxDist: Double = 4
  ): ChunkLoadingPrioritizer

  protected def distSqFuncDefault(p: PosAndDir, c: ChunkRelWorld): Double =
    p.pos.distanceSq(BlockCoords(BlockRelWorld(8, 8, 8, c)).toCylCoords)

  protected def makePos(x: Int, y: Int, z: Int) = PosAndDir(BlockCoords(x, y, z).toCylCoords)

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
        assertEquals((newChunk, newChunk.neighbors.exists(chunks.contains)), ((newChunk, true)))

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
    origin.pos = makePos(10 * 16, 0, 0).pos
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
}
