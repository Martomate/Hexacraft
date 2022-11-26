package com.martomate.hexacraft.world.chunk.storage

import com.flowpowered.nbt.CompoundTag
import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.{BlockFactory, BlockLoader, Blocks}
import com.martomate.hexacraft.world.chunk.storage.{ChunkStorage, DenseChunkStorage, SparseChunkStorage}
import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld
import org.scalatest.matchers.should.Matchers

class SparseChunkStorageTest extends ChunkStorageTest(SparseChunkStorage) with Matchers {
  "isDense" should "be false" in {
    val storage = new SparseChunkStorage

    storage.isDense shouldBe false
  }

  "the copy constructor" should "accept this storage type" in {
    val storage = new SparseChunkStorage
    fillStorage_Dirt359_Stone350(storage)
    val sparse = SparseChunkStorage.fromStorage(storage)

    sparse shouldBe a[SparseChunkStorage]
    sparse.numBlocks shouldBe 2
  }

  it should "accept other storage types" in {
    val storage = new DenseChunkStorage
    fillStorage_Dirt359_Stone350(storage)
    val sparse = SparseChunkStorage.fromStorage(storage)

    sparse shouldBe a[SparseChunkStorage]
    sparse.numBlocks shouldBe 2
  }
}
