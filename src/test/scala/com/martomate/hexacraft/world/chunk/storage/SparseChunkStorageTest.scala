package com.martomate.hexacraft.world.chunk.storage

import com.martomate.hexacraft.world.block.{BlockLoader, Blocks}
import com.martomate.hexacraft.world.chunk.storage.{ChunkStorage, DenseChunkStorage, SparseChunkStorage}
import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld

import com.flowpowered.nbt.CompoundTag
import com.martomate.hexacraft.world.CylinderSize

class SparseChunkStorageTest extends ChunkStorageTest(new SparseChunkStorage) {
  test("isDense should be false") {
    val storage = new SparseChunkStorage

    assert(!storage.isDense)
  }

  test("the copy constructor should accept this storage type") {
    val storage = new SparseChunkStorage
    fillStorage_Dirt359_Stone350(storage)
    val sparse = SparseChunkStorage.fromStorage(storage)

    assert(sparse.isInstanceOf[SparseChunkStorage])
    assertEquals(sparse.numBlocks, 2)
  }

  test("the copy constructor should accept other storage types") {
    val storage = new DenseChunkStorage
    fillStorage_Dirt359_Stone350(storage)
    val sparse = SparseChunkStorage.fromStorage(storage)

    assert(sparse.isInstanceOf[SparseChunkStorage])
    assertEquals(sparse.numBlocks, 2)
  }
}
