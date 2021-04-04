package com.martomate.hexacraft.world.storage

import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld
import org.scalatest.matchers.should.Matchers

class SparseChunkStorageTest extends ChunkStorageTest((coords, size) => new SparseChunkStorage(coords)(implicitly(size))) with Matchers {
  import cylSize.impl
  
  val makeOtherStorage: ChunkRelWorld => ChunkStorage = new DenseChunkStorage(_)

  "isDense" should "be false" in {
    val storage = makeStorage()

    storage.isDense shouldBe false
  }

  "the copy constructor" should "accept this storage type" in {
    val storage = makeStorage(cc0)
    fillStorage_Dirt359_Stone350(storage)
    val sparse = new SparseChunkStorage(storage)

    sparse shouldBe a [SparseChunkStorage]
    sparse.numBlocks shouldBe 2
  }

  it should "accept other storage types" in {
    val storage = makeOtherStorage(cc0)
    fillStorage_Dirt359_Stone350(storage)
    val sparse = new SparseChunkStorage(storage)

    sparse shouldBe a [SparseChunkStorage]
    sparse.numBlocks shouldBe 2
  }
}
