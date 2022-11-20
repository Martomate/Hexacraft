package com.martomate.hexacraft.world.chunk.storage

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.{BlockFactory, BlockLoader, Blocks}
import com.martomate.hexacraft.world.chunk.storage.{ChunkStorage, DenseChunkStorage, SparseChunkStorage}
import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld
import org.scalatest.matchers.should.Matchers

class DenseChunkStorageTest
    extends ChunkStorageTest(
      (coords, size, _) => new DenseChunkStorage(coords)(using size),
      (nbt, coords, size, _) => DenseChunkStorage.fromNBT(nbt)(coords)(using size)
    )
    with Matchers {

  val makeOtherStorage: ChunkRelWorld => ChunkStorage = new SparseChunkStorage(_)

  "isDense" should "be true" in {
    val storage = makeStorage()

    storage.isDense shouldBe true
  }

  "the copy constructor" should "accept this storage type" in {
    val storage = makeStorage(cc0)
    fillStorage_Dirt359_Stone350(storage)
    val dense = DenseChunkStorage.fromStorage(storage)

    dense shouldBe a[DenseChunkStorage]
    dense.numBlocks shouldBe 2
  }

  it should "accept other storage types" in {
    val storage = makeOtherStorage(cc0)
    fillStorage_Dirt359_Stone350(storage)
    val dense = DenseChunkStorage.fromStorage(storage)

    dense shouldBe a[DenseChunkStorage]
    dense.numBlocks shouldBe 2
  }
}
