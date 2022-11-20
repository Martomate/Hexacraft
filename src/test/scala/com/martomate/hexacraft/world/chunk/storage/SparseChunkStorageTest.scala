package com.martomate.hexacraft.world.chunk.storage

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.{BlockFactory, BlockLoader, Blocks}
import com.martomate.hexacraft.world.chunk.storage.{ChunkStorage, DenseChunkStorage, SparseChunkStorage}
import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld
import org.scalatest.matchers.should.Matchers

class SparseChunkStorageTest
    extends ChunkStorageTest(
      (coords, size, blocks) => new SparseChunkStorage(coords)(using size, blocks),
      (nbt, coords, size, blocks) => SparseChunkStorage.fromNBT(nbt)(coords)(using size, blocks)
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
    val sparse = SparseChunkStorage.fromStorage(storage)

    sparse shouldBe a[SparseChunkStorage]
    sparse.numBlocks shouldBe 2
  }

  it should "accept other storage types" in {
    val storage = makeOtherStorage(cc0)
    fillStorage_Dirt359_Stone350(storage)
    val sparse = SparseChunkStorage.fromStorage(storage)

    sparse shouldBe a[SparseChunkStorage]
    sparse.numBlocks shouldBe 2
  }
}
