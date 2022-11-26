package com.martomate.hexacraft.world.chunk.storage

import com.flowpowered.nbt.{ByteArrayTag, CompoundTag}
import com.martomate.hexacraft.util.{CylinderSize, NBTUtil}
import com.martomate.hexacraft.world.FakeBlockLoader
import com.martomate.hexacraft.world.block.{BlockFactory, BlockLoader, BlockState, Blocks}
import com.martomate.hexacraft.world.chunk.storage.{ChunkStorage, LocalBlockState}
import com.martomate.hexacraft.world.coord.integer.{BlockRelChunk, BlockRelWorld, ChunkRelWorld}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

abstract class ChunkStorageTest(storageFactory: ChunkStorageFactory) extends AnyFlatSpec with Matchers {
  protected implicit val cylSize: CylinderSize = CylinderSize(4)
  given BlockLoader = new FakeBlockLoader
  given BlockFactory = new BlockFactory
  implicit val Blocks: Blocks = new Blocks

  "the storage" should "be correct for 0 blocks" in {
    val storage = storageFactory.empty

    storage.numBlocks shouldBe 0
  }

  it should "be correct for 1 block" in {
    val storage = storageFactory.empty
    val coords = coords350.getBlockRelChunk

    storage.setBlock(coords, new BlockState(Blocks.Dirt))
    storage.setBlock(coords, new BlockState(Blocks.Dirt))

    storage.numBlocks shouldBe 1
  }

  it should "be correct for many blocks" in {
    val storage = storageFactory.empty

    for (i <- 0 until 16; j <- 0 until 16; k <- 0 until 16)
      storage.setBlock(BlockRelChunk(i, j, k), new BlockState(Blocks.Dirt))

    storage.numBlocks shouldBe 16 * 16 * 16
  }

  "Air" should "not count as a block" in {
    val storage = storageFactory.empty
    val coords = coords350.getBlockRelChunk

    storage.setBlock(coords, new BlockState(Blocks.Dirt))
    storage.setBlock(coords, new BlockState(Blocks.Air))

    storage.numBlocks shouldBe 0
  }

  "blockType" should "be correct for existing block" in {
    val storage: ChunkStorage = makeStorage_Dirt359_Stone350

    storage.blockType(coords350.getBlockRelChunk) shouldBe Blocks.Stone
  }

  it should "be Air for non-existing block" in {
    val storage: ChunkStorage = makeStorage_Dirt359_Stone350

    storage.blockType(coords351.getBlockRelChunk) shouldBe Blocks.Air
  }

  "removeBlock" should "work for existing blocks" in {
    val storage: ChunkStorage = makeStorage_Dirt359_Stone350

    storage.removeBlock(coords350.getBlockRelChunk)

    storage.blockType(coords350.getBlockRelChunk) shouldBe Blocks.Air
    storage.numBlocks shouldBe 1
  }

  it should "do nothing for non-existing blocks" in {
    val storage: ChunkStorage = makeStorage_Dirt359_Stone350

    storage.removeBlock(coords351.getBlockRelChunk)

    storage.blockType(coords350.getBlockRelChunk) shouldBe Blocks.Stone
    storage.numBlocks shouldBe 2
  }

  "getBlock" should "work for existing blocks" in {
    val storage = makeStorage_Dirt359_Stone350
    val block = storage.getBlock(coords350.getBlockRelChunk)

    block.blockType shouldBe Blocks.Stone
    block.metadata shouldBe 2
  }

  it should "return Air for non-existing block" in {
    val storage = makeStorage_Dirt359_Stone350
    val block = storage.getBlock(coords351.getBlockRelChunk)

    block.blockType shouldBe Blocks.Air
  }

  "allBlocks" should "return all blocks" in {
    val storage = makeStorage_Dirt359_Stone350
    val storageSize = storage.numBlocks

    val all = storage.allBlocks

    all.size shouldBe storageSize
    for (LocalBlockState(c, b) <- all)
      storage.getBlock(c) shouldBe b
  }

  "fromNBT" should "work with blocks and metadata" in {
    val tag = NBTUtil.makeCompoundTag(
      "",
      Seq(
        new ByteArrayTag(
          "blocks",
          Array.tabulate(16 * 16 * 16) {
            case 0 => Blocks.Dirt.id
            case 1 => Blocks.Stone.id
            case _ => 0
          }
        ),
        new ByteArrayTag(
          "metadata",
          Array.tabulate(16 * 16 * 16) {
            case 0 => 6
            case 1 => 2
            case _ => 0
          }
        )
      )
    )
    val storage = storageFactory.fromNBT(tag)

    storage.numBlocks shouldBe 2
    storage.blockType(coordsAt(0, 0, 1).getBlockRelChunk) shouldBe Blocks.Stone
  }

  it should "work without metadata" in {
    val tag = NBTUtil.makeCompoundTag(
      "",
      Seq(
        new ByteArrayTag(
          "blocks",
          Array.tabulate(16 * 16 * 16) {
            case 0 => Blocks.Dirt.id
            case 1 => Blocks.Stone.id
            case _ => 0
          }
        )
      )
    )
    val storage = storageFactory.fromNBT(tag)

    storage.numBlocks shouldBe 2
    storage.blockType(coordsAt(0, 0, 1).getBlockRelChunk) shouldBe Blocks.Stone
  }

  it should "work without blocks" in {
    val tag = NBTUtil.makeCompoundTag(
      "",
      Seq(
        new ByteArrayTag(
          "metadata",
          Array.tabulate(16 * 16 * 16) {
            case 0 => 6
            case 1 => 2
            case _ => 0
          }
        )
      )
    )
    val storage = storageFactory.fromNBT(tag)

    storage.numBlocks shouldBe 0
    storage.blockType(coordsAt(0, 0, 1).getBlockRelChunk) shouldBe Blocks.Air
  }

  "toNBT" should "work" in {
    val storage = makeStorage_Dirt359_Stone350
    val nbt = storage.toNBT
    nbt.size shouldBe 2

    val blocksTag = nbt(0)
    blocksTag.getName shouldBe "blocks"
    blocksTag shouldBe a[ByteArrayTag]
    val blocksArray = blocksTag.asInstanceOf[ByteArrayTag].getValue
    blocksArray.length shouldBe 16 * 16 * 16

    val metadataTag = nbt(1)
    metadataTag.getName shouldBe "metadata"
    metadataTag shouldBe a[ByteArrayTag]
    val metadataArray = metadataTag.asInstanceOf[ByteArrayTag].getValue
    metadataArray.length shouldBe 16 * 16 * 16

    blocksArray(coords359.getBlockRelChunk.value) shouldBe Blocks.Dirt.id
    blocksArray(coords350.getBlockRelChunk.value) shouldBe Blocks.Stone.id
    metadataArray(coords359.getBlockRelChunk.value) shouldBe 6
    metadataArray(coords350.getBlockRelChunk.value) shouldBe 2
  }

  protected def coords350: BlockRelWorld = coordsAt(3, 5, 0)
  protected def coords351: BlockRelWorld = coordsAt(3, 5, 1)
  protected def coords359: BlockRelWorld = coordsAt(3, 5, 9)
  protected def coordsAt(x: Int, y: Int, z: Int): BlockRelWorld = BlockRelWorld(x, y, z)
  protected def cc0: ChunkRelWorld = ChunkRelWorld(0)

  protected def makeStorage_Dirt359_Stone350: ChunkStorage = {
    val storage = storageFactory.empty
    fillStorage_Dirt359_Stone350(storage)
    storage
  }

  protected def fillStorage_Dirt359_Stone350(storage: ChunkStorage): Unit = {
    storage.setBlock(coords359.getBlockRelChunk, new BlockState(Blocks.Dirt, 6))
    storage.setBlock(coords350.getBlockRelChunk, new BlockState(Blocks.Stone, 2))
  }
}
