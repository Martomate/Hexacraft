package com.martomate.hexacraft.world.chunk.storage

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.{BlockFactory, BlockLoader, Blocks}
import com.martomate.hexacraft.world.chunk.storage.{ChunkStorage, DenseChunkStorage, SparseChunkStorage}
import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld

import com.flowpowered.nbt.CompoundTag
import org.scalatest.matchers.should.Matchers

class DenseChunkStorageTest extends ChunkStorageTest(DenseChunkStorage) with Matchers {
  "isDense" should "be true" in {
    val storage = new DenseChunkStorage

    storage.isDense shouldBe true
  }

  "the copy constructor" should "accept this storage type" in {
    val storage = new DenseChunkStorage
    fillStorage_Dirt359_Stone350(storage)
    val dense = DenseChunkStorage.fromStorage(storage)

    dense shouldBe a[DenseChunkStorage]
    dense.numBlocks shouldBe 2
  }

  it should "accept other storage types" in {
    val storage = new SparseChunkStorage
    fillStorage_Dirt359_Stone350(storage)
    val dense = DenseChunkStorage.fromStorage(storage)

    dense shouldBe a[DenseChunkStorage]
    dense.numBlocks shouldBe 2
  }
}
