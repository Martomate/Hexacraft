package com.martomate.hexacraft.world.loader

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld

import munit.FunSuite
import org.mockito.Mockito.verify
import org.scalatestplus.mockito.MockitoSugar

class ChunkLoadingEdgeTest extends FunSuite with MockitoSugar {
  implicit val cylSize: CylinderSize = CylinderSize(4)

  test("isLoaded should be false in the beginning") {
    val edge = new ChunkLoadingEdge
    assert(!edge.isLoaded(ChunkRelWorld(2, 4, 3)))
    assert(!edge.isLoaded(ChunkRelWorld(0, 0, 0)))
    assert(!edge.isLoaded(ChunkRelWorld(2, 4, 3)))
    assert(!edge.isLoaded(ChunkRelWorld(2, -4, -3)))
  }
  test("isLoaded should return true after loading") {
    val edge = new ChunkLoadingEdge
    assert(!edge.isLoaded(ChunkRelWorld(2, 4, 3)))
    edge.loadChunk(ChunkRelWorld(2, 4, 3))
    assert(!edge.isLoaded(ChunkRelWorld(0, 0, 0)))
    assert(edge.isLoaded(ChunkRelWorld(2, 4, 3)))
  }
  test("isLoaded should return false after unloading") {
    val edge = new ChunkLoadingEdge
    assert(!edge.isLoaded(ChunkRelWorld(2, 4, 3)))
    edge.loadChunk(ChunkRelWorld(2, 4, 3))
    edge.unloadChunk(ChunkRelWorld(2, 4, 3))
    assert(!edge.isLoaded(ChunkRelWorld(2, 4, 3)))
  }

  test("onEdge should be false in the beginning") {
    val edge = new ChunkLoadingEdge
    assert(!edge.onEdge(ChunkRelWorld(5, 6, 4)))
    assert(!edge.onEdge(ChunkRelWorld(0, 0, 0)))
  }
  test("onEdge should be true for the neighbors after adding one block and it's neighbors") {
    val edge = new ChunkLoadingEdge
    val coords = ChunkRelWorld(4, 6, 5)

    edge.loadChunk(coords)
    for (n <- coords.neighbors)
      edge.loadChunk(n)

    assert(!edge.onEdge(coords))
    for (n <- coords.neighbors)
      assert(edge.onEdge(n))
  }
  test("onEdge should be false after removing the last block") {
    val edge = new ChunkLoadingEdge
    val coords = ChunkRelWorld(4, 6, 5)
    edge.loadChunk(coords)
    for (n <- coords.neighbors)
      edge.loadChunk(n)

    for (n <- coords.neighbors)
      edge.unloadChunk(n)

    for (n <- coords.neighbors)
      assert(!edge.onEdge(n))

    assert(edge.onEdge(coords))
    edge.unloadChunk(coords)
    assert(!edge.onEdge(coords))
  }

  test("canLoad should be false in the beginning") {
    val edge = new ChunkLoadingEdge
    assert(!edge.canLoad(ChunkRelWorld(8, 2, 5)))
    assert(!edge.canLoad(ChunkRelWorld(0, 0, 0)))
  }

  test("canLoad should be true for the neighbors after adding one block") {
    val edge = new ChunkLoadingEdge
    val coords = ChunkRelWorld(4, 6, 5)

    edge.loadChunk(coords)

    assert(!edge.canLoad(coords))
    for (n <- coords.neighbors)
      assert(edge.canLoad(n))
  }

  test("canLoad should be false after loading the chunk") {
    val edge = new ChunkLoadingEdge
    val first = ChunkRelWorld(4, 6, 4)
    val coords = ChunkRelWorld(4, 6, 5)

    edge.loadChunk(first)
    edge.loadChunk(coords)

    assert(!edge.canLoad(coords))
  }

  test("canLoad should be true after removing a block") {
    val edge = new ChunkLoadingEdge
    val first = ChunkRelWorld(4, 6, 4)
    val coords = ChunkRelWorld(4, 6, 5)

    edge.loadChunk(first)
    edge.loadChunk(coords)
    edge.unloadChunk(coords)

    assert(edge.canLoad(coords))
  }

  test("canLoad should be false after removing the last block") {
    val edge = new ChunkLoadingEdge
    edge.loadChunk(ChunkRelWorld(0, 0, 0))
    edge.loadChunk(ChunkRelWorld(2, 5, 4))
    edge.loadChunk(ChunkRelWorld(2, -2, 7))
    edge.unloadChunk(ChunkRelWorld(0, 0, 0))
    edge.unloadChunk(ChunkRelWorld(2, 5, 4))
    edge.unloadChunk(ChunkRelWorld(2, -2, 7))

    assert(!edge.canLoad(ChunkRelWorld(0, 0, 0)))
    assert(!edge.canLoad(ChunkRelWorld(1, 0, 0)))
    assert(!edge.canLoad(ChunkRelWorld(2, 5, 4)))
    assert(!edge.canLoad(ChunkRelWorld(3, 5, 4)))
    assert(!edge.canLoad(ChunkRelWorld(2, -2, 7)))
    assert(!edge.canLoad(ChunkRelWorld(1, -2, 7)))
  }

  test("listeners should be called on load") {
    val edge = new ChunkLoadingEdge
    val listener = mock[ChunkLoadingEdgeListener]
    edge.addListener(listener)

    edge.loadChunk(ChunkRelWorld(2, 4, 3))

    val coords = ChunkRelWorld(2, 4, 3)
    verify(listener).onChunkOnEdge(coords, onEdge = true)
    for (n <- coords.neighbors)
      verify(listener).onChunkLoadable(n, loadable = true)
  }
  test("listeners should be called on remove") {
    val edge = new ChunkLoadingEdge
    edge.loadChunk(ChunkRelWorld(2, 4, 3))

    val listener = mock[ChunkLoadingEdgeListener]
    edge.addListener(listener)

    edge.unloadChunk(ChunkRelWorld(2, 4, 3))

    val coords = ChunkRelWorld(2, 4, 3)
    verify(listener).onChunkOnEdge(coords, onEdge = false)
    for (n <- coords.neighbors)
      verify(listener).onChunkLoadable(n, loadable = false)
  }

  test("removeListener should remove the listener") {
    val edge = new ChunkLoadingEdge
    val listener = mock[ChunkLoadingEdgeListener]
    edge.addListener(listener)
    edge.removeListener(listener)

    edge.loadChunk(ChunkRelWorld(2, 4, 3))
  }
}
