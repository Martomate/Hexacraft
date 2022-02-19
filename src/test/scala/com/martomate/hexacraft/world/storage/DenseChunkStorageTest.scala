package com.martomate.hexacraft.world.storage

import com.martomate.hexacraft.world.chunk.storage.{ChunkStorage, DenseChunkStorage, SparseChunkStorage}
import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld
import org.scalatest.matchers.should.Matchers

class DenseChunkStorageTest extends ChunkStorageTest((coords, size) => new DenseChunkStorage(coords)(implicitly(size))) with Matchers {
  import cylSize.impl

  val makeOtherStorage: ChunkRelWorld => ChunkStorage = new SparseChunkStorage(_)

  "isDense" should "be true" in {
    val storage = makeStorage()

    storage.isDense shouldBe true
  }

  "the copy constructor" should "accept this storage type" in {
    val storage = makeStorage(cc0)
    fillStorage_Dirt359_Stone350(storage)
    val dense = new DenseChunkStorage(storage)

    dense shouldBe a [DenseChunkStorage]
    dense.numBlocks shouldBe 2
  }

  it should "accept other storage types" in {
    val storage = makeOtherStorage(cc0)
    fillStorage_Dirt359_Stone350(storage)
    val dense = new DenseChunkStorage(storage)

    dense shouldBe a [DenseChunkStorage]
    dense.numBlocks shouldBe 2
  }
}
