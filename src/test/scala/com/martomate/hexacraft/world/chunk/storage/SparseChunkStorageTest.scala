package com.martomate.hexacraft.world.chunk.storage

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.{BlockFactory, BlockLoader, Blocks}
import com.martomate.hexacraft.world.chunk.storage.{ChunkStorage, DenseChunkStorage, SparseChunkStorage}
import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld
import org.scalatest.matchers.should.Matchers

class SparseChunkStorageTest
    extends ChunkStorageTest((coords, size, blocks) =>
      given CylinderSize = size
      given Blocks = blocks
      new SparseChunkStorage(coords)
    )
    with Matchers {
  val makeOtherStorage: ChunkRelWorld => ChunkStorage = new DenseChunkStorage(_)

  "isDense" should "be false" in {
    val storage = makeStorage()

    storage.isDense shouldBe false
  }

  "the copy constructor" should "accept this storage type" in {
    val storage = makeStorage(cc0)
    fillStorage_Dirt359_Stone350(storage)
    val sparse = new SparseChunkStorage(storage)

    sparse shouldBe a[SparseChunkStorage]
    sparse.numBlocks shouldBe 2
  }

  it should "accept other storage types" in {
    val storage = makeOtherStorage(cc0)
    fillStorage_Dirt359_Stone350(storage)
    val sparse = new SparseChunkStorage(storage)

    sparse shouldBe a[SparseChunkStorage]
    sparse.numBlocks shouldBe 2
  }
}
