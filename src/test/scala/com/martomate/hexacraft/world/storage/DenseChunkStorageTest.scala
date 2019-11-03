package com.martomate.hexacraft.world.storage

import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld

class DenseChunkStorageTest extends ChunkStorageTest((coords, size) => new DenseChunkStorage(coords)(implicitly(size))) {
  import cylSize.impl

  val makeOtherStorage: ChunkRelWorld => ChunkStorage = new SparseChunkStorage(_)

  test("Is dense") {
    val storage = makeStorage()
    assert(storage.isDense)
  }

  test("copy constructor can take this storage type") {
    val storage = makeStorage(cc0)
    fillStorage_Dirt359_Stone350(storage)
    val dense = new DenseChunkStorage(storage)
    assert(dense.isInstanceOf[DenseChunkStorage])
    assertResult(2)(dense.numBlocks)
  }

  test("copy constructor can take other storage types") {
    val storage = makeOtherStorage(cc0)
    fillStorage_Dirt359_Stone350(storage)
    val dense = new DenseChunkStorage(storage)
    assert(dense.isInstanceOf[DenseChunkStorage])
    assertResult(2)(dense.numBlocks)
  }
}
