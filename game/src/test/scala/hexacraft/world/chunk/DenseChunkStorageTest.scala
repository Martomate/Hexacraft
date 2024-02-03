package hexacraft.world.chunk

import hexacraft.world.block.Block

class DenseChunkStorageTest extends ChunkStorageTest(new DenseChunkStorage) {
  test("isDense should be true") {
    val storage = new DenseChunkStorage

    assert(storage.isDense)
  }

  test("fromStorage accepts this storage type") {
    val storage = new DenseChunkStorage
    fillStorage_Dirt359_Stone350(storage)
    val dense = DenseChunkStorage.fromStorage(storage)

    assert(dense.isInstanceOf[DenseChunkStorage])
    assertEquals(dense.numBlocks, 2)
  }

  test("fromStorage accepts other storage types") {
    val storage = new SparseChunkStorage
    fillStorage_Dirt359_Stone350(storage)
    val dense = DenseChunkStorage.fromStorage(storage)

    assert(dense.isInstanceOf[DenseChunkStorage])
    assertEquals(dense.numBlocks, 2)
  }

  test("create works") {
    val storage = DenseChunkStorage.create(
      Array.tabulate(16 * 16 * 16) {
        case 0 => Block.Dirt.id
        case 1 => Block.Stone.id
        case _ => 0
      },
      Array.tabulate(16 * 16 * 16) {
        case 0 => 6
        case 1 => 2
        case _ => 0
      }
    )

    assertEquals(storage.numBlocks, 2)
    assertEquals(storage.blockType(coordsAt(0, 0, 1).getBlockRelChunk), Block.Stone)
  }
}
