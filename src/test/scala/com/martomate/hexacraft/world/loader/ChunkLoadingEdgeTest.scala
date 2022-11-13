package com.martomate.hexacraft.world.loader

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld
import org.mockito.Mockito.verify
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

class ChunkLoadingEdgeTest extends AnyFlatSpec with Matchers with MockitoSugar {
  implicit val cylSize : CylinderSize = CylinderSize(4)

  "isLoaded" should "be false in the beginning" in {
    val edge = new ChunkLoadingEdge
    edge.isLoaded(ChunkRelWorld(2, 4, 3)) shouldBe false
    edge.isLoaded(ChunkRelWorld(0, 0, 0)) shouldBe false
    edge.isLoaded(ChunkRelWorld(2, 4, 3)) shouldBe false
    edge.isLoaded(ChunkRelWorld(2,-4,-3)) shouldBe false
  }
  it should "return true after loading" in {
    val edge = new ChunkLoadingEdge
    edge.isLoaded(ChunkRelWorld(2, 4, 3)) shouldBe false
    edge.loadChunk(ChunkRelWorld(2, 4, 3))
    edge.isLoaded(ChunkRelWorld(0, 0, 0)) shouldBe false
    edge.isLoaded(ChunkRelWorld(2, 4, 3)) shouldBe true
  }
  it should "return false after unloading" in {
    val edge = new ChunkLoadingEdge
    edge.isLoaded(ChunkRelWorld(2, 4, 3)) shouldBe false
    edge.loadChunk(ChunkRelWorld(2, 4, 3))
    edge.unloadChunk(ChunkRelWorld(2, 4, 3))
    edge.isLoaded(ChunkRelWorld(2, 4, 3)) shouldBe false
  }

  "onEdge" should "be false in the beginning" in {
    val edge = new ChunkLoadingEdge
    edge.onEdge(ChunkRelWorld(5, 6, 4)) shouldBe false
    edge.onEdge(ChunkRelWorld(0, 0, 0)) shouldBe false
  }
  it should "be true for the neighbors after adding one block and it's neighbors" in {
    val edge = new ChunkLoadingEdge
    val coords = ChunkRelWorld(4, 6, 5)

    edge.loadChunk(coords)
    for (n <- coords.neighbors)
      edge.loadChunk(n)

    edge.onEdge(coords) shouldBe false
    for (n <- coords.neighbors)
      edge.onEdge(n) shouldBe true
  }
  it should "be false after removing the last block" in {
    val edge = new ChunkLoadingEdge
    val coords = ChunkRelWorld(4, 6, 5)
    edge.loadChunk(coords)
    for (n <- coords.neighbors)
      edge.loadChunk(n)

    for (n <- coords.neighbors)
      edge.unloadChunk(n)

    for (n <- coords.neighbors)
      edge.onEdge(n) shouldBe false

    edge.onEdge(coords) shouldBe true
    edge.unloadChunk(coords)
    edge.onEdge(coords) shouldBe false
  }

  "canLoad" should "be false in the beginning" in {
    val edge = new ChunkLoadingEdge
    edge.canLoad(ChunkRelWorld(8, 2, 5)) shouldBe false
    edge.canLoad(ChunkRelWorld(0, 0, 0)) shouldBe false
  }

  it should "be true for the neighbors after adding one block" in {
    val edge = new ChunkLoadingEdge
    val coords = ChunkRelWorld(4, 6, 5)

    edge.loadChunk(coords)

    edge.canLoad(coords) shouldBe false
    for (n <- coords.neighbors)
      edge.canLoad(n) shouldBe true
  }

  it should "be false after loading the chunk" in {
    val edge = new ChunkLoadingEdge
    val first = ChunkRelWorld(4, 6, 4)
    val coords = ChunkRelWorld(4, 6, 5)

    edge.loadChunk(first)
    edge.loadChunk(coords)

    edge.canLoad(coords) shouldBe false
  }

  it should "be true after removing a block" in {
    val edge = new ChunkLoadingEdge
    val first = ChunkRelWorld(4, 6, 4)
    val coords = ChunkRelWorld(4, 6, 5)

    edge.loadChunk(first)
    edge.loadChunk(coords)
    edge.unloadChunk(coords)

    edge.canLoad(coords) shouldBe true
  }

  it should "be false after removing the last block" in {
    val edge = new ChunkLoadingEdge
    edge.loadChunk(ChunkRelWorld(0,0,0))
    edge.loadChunk(ChunkRelWorld(2,5,4))
    edge.loadChunk(ChunkRelWorld(2,-2,7))
    edge.unloadChunk(ChunkRelWorld(0,0,0))
    edge.unloadChunk(ChunkRelWorld(2,5,4))
    edge.unloadChunk(ChunkRelWorld(2,-2,7))

    edge.canLoad(ChunkRelWorld(0,0,0)) shouldBe false
    edge.canLoad(ChunkRelWorld(1,0,0)) shouldBe false
    edge.canLoad(ChunkRelWorld(2,5,4)) shouldBe false
    edge.canLoad(ChunkRelWorld(3,5,4)) shouldBe false
    edge.canLoad(ChunkRelWorld(2,-2,7)) shouldBe false
    edge.canLoad(ChunkRelWorld(1,-2,7)) shouldBe false
  }

  "listeners" should "be called on load" in {
    val edge = new ChunkLoadingEdge
    val listener = mock[ChunkLoadingEdgeListener]
    edge.addListener(listener)

    edge.loadChunk(ChunkRelWorld(2,4,3))

    val coords = ChunkRelWorld(2,4,3)
    verify(listener).onChunkOnEdge(coords, onEdge = true)
    for (n <- coords.neighbors)
      verify(listener).onChunkLoadable(n, loadable = true)
  }
  it should "be called on remove"  in {
    val edge = new ChunkLoadingEdge
    edge.loadChunk(ChunkRelWorld(2,4,3))

    val listener = mock[ChunkLoadingEdgeListener]
    edge.addListener(listener)

    edge.unloadChunk(ChunkRelWorld(2,4,3))

    val coords = ChunkRelWorld(2,4,3)
    verify(listener).onChunkOnEdge(coords, onEdge = false)
    for (n <- coords.neighbors)
      verify(listener).onChunkLoadable(n, loadable = false)
  }

  "removeListener" should "remove the listener" in {
    val edge = new ChunkLoadingEdge
    val listener = mock[ChunkLoadingEdgeListener]
    edge.addListener(listener)
    edge.removeListener(listener)

    edge.loadChunk(ChunkRelWorld(2,4,3))
  }
}
