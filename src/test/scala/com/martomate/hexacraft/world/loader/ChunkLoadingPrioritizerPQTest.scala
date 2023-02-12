package com.martomate.hexacraft.world.loader

import com.martomate.hexacraft.util.SeqUtils
import com.martomate.hexacraft.world.coord.fp.BlockCoords
import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld

import scala.collection.mutable

class ChunkLoadingPrioritizerPQTest extends ChunkLoadingPrioritizerTest {
  override def make(
      origin: PosAndDir,
      distSqFunc: (PosAndDir, ChunkRelWorld) => Double,
      maxDist: Double
  ): ChunkLoadingPrioritizer =
    new ChunkLoadingPrioritizerPQ(origin, distSqFunc, maxDist)

  test("nextAddableChunk should return chunks in order of increasing distance after adding the first one") {
    val origin = makePos(0, 0, 0)
    val prio = make(origin)
    prio += prio.nextAddableChunk.get
    var prevDistSq: Double = 0
    SeqUtils.whileSome(10000, prio.nextAddableChunk) { chunk =>
      val distSq = distSqFuncDefault(origin, chunk)
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
      mutable.HashSet(
        ChunkRelWorld(0, 0, 0)
          .extendedNeighbors(4)
          .filter(c => distSqFuncDefault(origin, c) <= maxDistSqInBlocks): _*
      )
    SeqUtils.whileSome(10000, prio.nextAddableChunk) { chunk =>
      prio += chunk
      chunks -= chunk
    }
    assert(chunks.isEmpty)
  }
  test("nextAddableChunk should retain order when origin is moved") {
    val origin = makePos(0, 0, 0)
    val prio = make(origin).asInstanceOf[ChunkLoadingPrioritizerPQ]
    prio += prio.nextAddableChunk.get

    assertEquals(nextAddableChunkWPos(32, 8, 8), Some(ChunkRelWorld(1, 0, 0)))
    assertEquals(nextAddableChunkWPos(-32, 8, 8), Some(ChunkRelWorld(-1, 0, 0)))
    assertEquals(nextAddableChunkWPos(8, 32, 8), Some(ChunkRelWorld(0, 1, 0)))
    assertEquals(nextAddableChunkWPos(8, -32, 8), Some(ChunkRelWorld(0, -1, 0)))
    assertEquals(nextAddableChunkWPos(8, 8, 32), Some(ChunkRelWorld(0, 0, 1)))
    assertEquals(nextAddableChunkWPos(8, 8, -32), Some(ChunkRelWorld(0, 0, -1)))

    def nextAddableChunkWPos(x: Int, y: Int, z: Int): Option[ChunkRelWorld] = {
      origin.pos = BlockCoords(x, y, z).toCylCoords
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
      val distSq = distSqFuncDefault(origin, chunk)
      assert(distSq <= prevDistSq)
      prevDistSq = distSq
      prio -= chunk
    }

    assertEquals(prio.nextRemovableChunk, None)
  }
  test("nextRemovableChunk should retain order when origin is moved") {
    val origin = makePos(0, 0, 0)
    val prio = make(origin, maxDist = 1).asInstanceOf[ChunkLoadingPrioritizerPQ]
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
      origin.pos = BlockCoords(x, y, z).toCylCoords
      prio.reorderPQs()
      prio.nextRemovableChunk
    }
  }
  test("nextRemovableChunk should retain order when origin is rotated".ignore) {}
}
