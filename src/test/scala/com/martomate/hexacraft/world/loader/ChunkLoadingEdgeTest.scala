package com.martomate.hexacraft.world.loader

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ChunkLoadingEdgeTest extends AnyFlatSpec with Matchers with MockFactory {
  implicit val cylSize : CylinderSize = new CylinderSize(4)

  "isLoaded" should "be false in the beginning" in {
    val edge = make
    edge.isLoaded(ChunkRelWorld(2, 4, 3)) shouldBe false
    edge.isLoaded(ChunkRelWorld(0, 0, 0)) shouldBe false
    edge.isLoaded(ChunkRelWorld(2, 4, 3)) shouldBe false
    edge.isLoaded(ChunkRelWorld(2,-4,-3)) shouldBe false
  }
  it should "return true after loading" in {
    val edge = make
    edge.isLoaded(ChunkRelWorld(2, 4, 3)) shouldBe false
    edge.loadChunk(ChunkRelWorld(2, 4, 3))
    edge.isLoaded(ChunkRelWorld(0, 0, 0)) shouldBe false
    edge.isLoaded(ChunkRelWorld(2, 4, 3)) shouldBe true
  }
  it should "return false after unloading" in {
    val edge = make
    edge.isLoaded(ChunkRelWorld(2, 4, 3)) shouldBe false
    edge.loadChunk(ChunkRelWorld(2, 4, 3))
    edge.unloadChunk(ChunkRelWorld(2, 4, 3))
    edge.isLoaded(ChunkRelWorld(2, 4, 3)) shouldBe false
  }

  "onEdge" should "be false in the beginning" in {
    val edge = make
    edge.onEdge(ChunkRelWorld(5, 6, 4)) shouldBe false
    edge.onEdge(ChunkRelWorld(0, 0, 0)) shouldBe false
  }
  it should "be true for the neighbors after adding one block and it's neighbors" in {
    val edge = make
    val coords = ChunkRelWorld(4, 6, 5)

    edge.loadChunk(coords)
    for (n <- coords.neighbors)
      edge.loadChunk(n)

    edge.onEdge(coords) shouldBe false
    for (n <- coords.neighbors)
      edge.onEdge(n) shouldBe true
  }
  it should "be false after removing the last block" in {
    val edge = make
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
    val edge = make
    edge.canLoad(ChunkRelWorld(8, 2, 5)) shouldBe false
    edge.canLoad(ChunkRelWorld(0, 0, 0)) shouldBe false
  }

  it should "be true for the neighbors after adding one block" in {
    val edge = make
    val coords = ChunkRelWorld(4, 6, 5)

    edge.loadChunk(coords)

    edge.canLoad(coords) shouldBe false
    for (n <- coords.neighbors)
      edge.canLoad(n) shouldBe true
  }

  it should "be false after loading the chunk" in {
    val edge = make
    val first = ChunkRelWorld(4, 6, 4)
    val coords = ChunkRelWorld(4, 6, 5)

    edge.loadChunk(first)
    edge.loadChunk(coords)

    edge.canLoad(coords) shouldBe false
  }

  it should "be true after removing a block" in {
    val edge = make
    val first = ChunkRelWorld(4, 6, 4)
    val coords = ChunkRelWorld(4, 6, 5)

    edge.loadChunk(first)
    edge.loadChunk(coords)
    edge.unloadChunk(coords)

    edge.canLoad(coords) shouldBe true
  }

  it should "be false after removing the last block" in {
    val edge = make
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
    val edge = make
    val listener = mock[ChunkLoadingEdgeListener]
    edge.addListener(listener)

    val coords = ChunkRelWorld(2,4,3)
    listener.onChunkOnEdge _ expects(coords, true)
    for (n <- coords.neighbors)
      listener.onChunkLoadable _ expects(n, true)

    edge.loadChunk(ChunkRelWorld(2,4,3))
  }
  it should "be called on remove"  in {
    val edge = make
    edge.loadChunk(ChunkRelWorld(2,4,3))

    val listener = mock[ChunkLoadingEdgeListener]
    edge.addListener(listener)

    val coords = ChunkRelWorld(2,4,3)
    listener.onChunkOnEdge _ expects(coords, false)
    for (n <- coords.neighbors)
      listener.onChunkLoadable _ expects(n, false)

    edge.unloadChunk(ChunkRelWorld(2,4,3))
  }

  "removeListener" should "remove the listener" in {
    val edge = make
    val listener = mock[ChunkLoadingEdgeListener]
    edge.addListener(listener)
    edge.removeListener(listener)

    edge.loadChunk(ChunkRelWorld(2,4,3))
  }

  def make: ChunkLoadingEdge = new ChunkLoadingEdge
}
