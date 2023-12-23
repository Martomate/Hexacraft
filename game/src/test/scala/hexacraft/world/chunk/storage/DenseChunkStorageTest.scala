package hexacraft.world.chunk.storage

import hexacraft.world.block.Block
import hexacraft.world.chunk.{DenseChunkStorage, SparseChunkStorage}

class DenseChunkStorageTest extends ChunkStorageTest(new DenseChunkStorage) {
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

  test("fromNBT should work with blocks and metadata") {
    val storage = DenseChunkStorage.fromNBT(
      Array.tabulate(16 * 16 * 16) {
        case 0 => Block.Dirt.id
        case 1 => Block.Stone.id
        case _ => 0
      },
      Some(Array.tabulate(16 * 16 * 16) {
        case 0 => 6
        case 1 => 2
        case _ => 0
      })
    )

    assertEquals(storage.numBlocks, 2)
    assertEquals(storage.blockType(coordsAt(0, 0, 1).getBlockRelChunk), Block.Stone)
  }

  test("fromNBT should work without metadata") {
    val storage = DenseChunkStorage.fromNBT(
      Array.tabulate(16 * 16 * 16) {
        case 0 => Block.Dirt.id
        case 1 => Block.Stone.id
        case _ => 0
      },
      None
    )

    assertEquals(storage.numBlocks, 2)
    assertEquals(storage.blockType(coordsAt(0, 0, 1).getBlockRelChunk), Block.Stone)
  }
}
