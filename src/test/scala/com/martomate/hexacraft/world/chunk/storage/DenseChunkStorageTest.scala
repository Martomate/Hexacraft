package com.martomate.hexacraft.world.chunk.storage

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.{BlockFactory, BlockLoader, Blocks}
import com.martomate.hexacraft.world.chunk.storage.{ChunkStorage, DenseChunkStorage, SparseChunkStorage}
import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld

import com.flowpowered.nbt.CompoundTag

class DenseChunkStorageTest extends ChunkStorageTest(DenseChunkStorage) {
  test("isDense should be true") {
    val storage = new DenseChunkStorage

    assert(storage.isDense)
  }

  test("the copy constructor should accept this storage type") {
    val storage = new DenseChunkStorage
    fillStorage_Dirt359_Stone350(storage)
    val dense = DenseChunkStorage.fromStorage(storage)

    assert(dense.isInstanceOf[DenseChunkStorage])
    assertEquals(dense.numBlocks, 2)
  }

  test("the copy constructor should accept other storage types") {
    val storage = new SparseChunkStorage
    fillStorage_Dirt359_Stone350(storage)
    val dense = DenseChunkStorage.fromStorage(storage)

    assert(dense.isInstanceOf[DenseChunkStorage])
    assertEquals(dense.numBlocks, 2)
  }
}
