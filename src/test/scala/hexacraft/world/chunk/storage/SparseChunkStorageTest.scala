package hexacraft.world.chunk.storage

import hexacraft.world.block.Blocks

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
