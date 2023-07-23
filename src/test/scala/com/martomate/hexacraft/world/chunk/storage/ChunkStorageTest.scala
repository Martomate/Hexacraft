package com.martomate.hexacraft.world.chunk.storage

import com.martomate.hexacraft.nbt.NBTUtil
import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.FakeBlockLoader
import com.martomate.hexacraft.world.block.{BlockLoader, Blocks, BlockState}
import com.martomate.hexacraft.world.chunk.storage.{ChunkStorage, LocalBlockState}
import com.martomate.hexacraft.world.coord.integer.{BlockRelChunk, BlockRelWorld, ChunkRelWorld}

import com.flowpowered.nbt.{ByteArrayTag, CompoundTag}
import munit.FunSuite

abstract class ChunkStorageTest(makeStorage: Blocks ?=> ChunkStorage) extends FunSuite {
  protected implicit val cylSize: CylinderSize = CylinderSize(4)
  given BlockLoader = new FakeBlockLoader
  implicit val Blocks: Blocks = new Blocks

  test("the storage should be correct for 0 blocks") {
    val storage = makeStorage

    assertEquals(storage.numBlocks, 0)
  }

  test("the storage should be correct for 1 block") {
    val storage = makeStorage
    val coords = coords350.getBlockRelChunk

    storage.setBlock(coords, new BlockState(Blocks.Dirt))
    storage.setBlock(coords, new BlockState(Blocks.Dirt))

    assertEquals(storage.numBlocks, 1)
  }

  test("the storage should be correct for many blocks") {
    val storage = makeStorage

    for (i <- 0 until 16; j <- 0 until 16; k <- 0 until 16)
      storage.setBlock(BlockRelChunk(i, j, k), new BlockState(Blocks.Dirt))

    assertEquals(storage.numBlocks, 16 * 16 * 16)
  }

  test("Air should not count as a block") {
    val storage = makeStorage
    val coords = coords350.getBlockRelChunk

    storage.setBlock(coords, new BlockState(Blocks.Dirt))
    storage.setBlock(coords, new BlockState(Blocks.Air))

    assertEquals(storage.numBlocks, 0)
  }

  test("blockType should be correct for existing block") {
    val storage: ChunkStorage = makeStorage_Dirt359_Stone350

    assertEquals(storage.blockType(coords350.getBlockRelChunk), Blocks.Stone)
  }

  test("blockType should be Air for non-existing block") {
    val storage: ChunkStorage = makeStorage_Dirt359_Stone350

    assertEquals(storage.blockType(coords351.getBlockRelChunk), Blocks.Air)
  }

  test("removeBlock should work for existing blocks") {
    val storage: ChunkStorage = makeStorage_Dirt359_Stone350

    storage.removeBlock(coords350.getBlockRelChunk)

    assertEquals(storage.blockType(coords350.getBlockRelChunk), Blocks.Air)
    assertEquals(storage.numBlocks, 1)
  }

  test("removeBlock should do nothing for non-existing blocks") {
    val storage: ChunkStorage = makeStorage_Dirt359_Stone350

    storage.removeBlock(coords351.getBlockRelChunk)

    assertEquals(storage.blockType(coords350.getBlockRelChunk), Blocks.Stone)
    assertEquals(storage.numBlocks, 2)
  }

  test("getBlock should work for existing blocks") {
    val storage = makeStorage_Dirt359_Stone350
    val block = storage.getBlock(coords350.getBlockRelChunk)

    assertEquals(block.blockType, Blocks.Stone)
    assertEquals(block.metadata, 2.toByte)
  }

  test("getBlock should return Air for non-existing block") {
    val storage = makeStorage_Dirt359_Stone350
    val block = storage.getBlock(coords351.getBlockRelChunk)

    assertEquals(block.blockType, Blocks.Air)
  }

  test("allBlocks should return all blocks") {
    val storage = makeStorage_Dirt359_Stone350
    val storageSize = storage.numBlocks

    val all = storage.allBlocks

    assertEquals(all.length, storageSize)
    for (LocalBlockState(c, b) <- all)
      assertEquals(storage.getBlock(c), b)
  }

  test("toNBT should work") {
    val storage = makeStorage_Dirt359_Stone350
    val nbt = storage.toNBT

    val blocksArray = nbt.blocks
    assertEquals(blocksArray.length, 16 * 16 * 16)

    val metadataArray = nbt.metadata
    assertEquals(metadataArray.length, 16 * 16 * 16)

    assertEquals(blocksArray(coords359.getBlockRelChunk.value), Blocks.Dirt.id)
    assertEquals(blocksArray(coords350.getBlockRelChunk.value), Blocks.Stone.id)
    assertEquals(metadataArray(coords359.getBlockRelChunk.value), 6.toByte)
    assertEquals(metadataArray(coords350.getBlockRelChunk.value), 2.toByte)
  }

  protected def coords350: BlockRelWorld = coordsAt(3, 5, 0)
  protected def coords351: BlockRelWorld = coordsAt(3, 5, 1)
  protected def coords359: BlockRelWorld = coordsAt(3, 5, 9)
  protected def coordsAt(x: Int, y: Int, z: Int): BlockRelWorld = BlockRelWorld(x, y, z)
  protected def cc0: ChunkRelWorld = ChunkRelWorld(0)

  protected def makeStorage_Dirt359_Stone350: ChunkStorage = {
    val storage = makeStorage
    fillStorage_Dirt359_Stone350(storage)
    storage
  }

  protected def fillStorage_Dirt359_Stone350(storage: ChunkStorage): Unit = {
    storage.setBlock(coords359.getBlockRelChunk, new BlockState(Blocks.Dirt, 6))
    storage.setBlock(coords350.getBlockRelChunk, new BlockState(Blocks.Stone, 2))
  }
}
