package com.martomate.hexacraft.world.storage

import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld

class SparseChunkStorageTest extends ChunkStorageTest(new SparseChunkStorage(_)) {
  val makeOtherStorage: ChunkRelWorld => ChunkStorage = new DenseChunkStorage(_)

  test("Is sparse") {
    val storage = makeStorage(null)
    assert(!storage.isDense)
  }

  test("copy constructor can take this storage type") {
    val storage = makeStorage(cc0)
    fillStorage_Dirt359_Stone350(storage)
    val sparse = new SparseChunkStorage(storage)
    assert(sparse.isInstanceOf[SparseChunkStorage])
    assertResult(2)(sparse.numBlocks)
  }

  test("copy constructor can take other storage types") {
    val storage = makeOtherStorage(cc0)
    fillStorage_Dirt359_Stone350(storage)
    val sparse = new SparseChunkStorage(storage)
    assert(sparse.isInstanceOf[SparseChunkStorage])
    assertResult(2)(sparse.numBlocks)
  }
}
