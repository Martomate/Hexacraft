package hexacraft.world.storage

import com.flowpowered.nbt.{ByteArrayTag, CompoundTag, Tag}
import hexacraft.block.{Block, BlockState}
import hexacraft.util.{DefaultArray, NBTUtil}
import hexacraft.world.coord.{BlockRelChunk, BlockRelWorld}

trait ChunkStorage {
  def blockType(coord: BlockRelChunk): Block

  def getBlock(coord: BlockRelChunk): Option[BlockState]
  def setBlock(block: BlockState): Unit
  def removeBlock(coords: BlockRelChunk): Unit

  def allBlocks: Seq[BlockState]
  def numBlocks: Int

  def isDense: Boolean
  def toDense: DenseChunkStorage
  def toSparse: SparseChunkStorage
  
  def fromNBT(nbt: CompoundTag): Unit
  def toNBT: Seq[Tag[_]]
}

class DenseChunkStorage(chunk: Chunk) extends ChunkStorage {
  private val blockTypes = new Array[Byte](16 * 16 * 16)
  private val metadata = new Array[Byte](16 * 16 * 16)
  private var _numBlocks = 0

  def blockType(coord: BlockRelChunk): Block = Block.byId(blockTypes(coord.value))
  def getBlock(coord: BlockRelChunk): Option[BlockState] = {
    val _type = blockTypes(coord.value)
    if (_type != 0) Some(new BlockState(coord.withChunk(chunk.coords), chunk.world, Block.byId(_type), metadata(coord.value))) else None
  }
  def setBlock(block: BlockState): Unit = {
    val idx = block.coords.getBlockRelChunk.value
    if (blockTypes(idx) == 0) _numBlocks += 1
    blockTypes(idx) = block.blockType.id
    metadata(idx) = block.metadata
    if (blockTypes(idx) == 0) _numBlocks -= 1
  }
  def removeBlock(coords: BlockRelChunk): Unit = {
    if (blockTypes(coords.value) != 0) _numBlocks -= 1
    blockTypes(coords.value) = 0
  }
  def allBlocks: Seq[BlockState] = blockTypes.indices.filter(i => blockTypes(i) != 0).map(i => 
    new BlockState(BlockRelChunk(i, chunk.world.size).withChunk(chunk.coords), chunk.world, Block.byId(blockTypes(i)), metadata(i)))
  def numBlocks: Int = _numBlocks
  def isDense: Boolean = true
  def toDense: DenseChunkStorage = this
  def toSparse: SparseChunkStorage = {
    val sparse = new SparseChunkStorage(chunk)
    for (b <- allBlocks) sparse.setBlock(b)
    sparse
  }

  def fromNBT(nbt: CompoundTag): Unit = {
    val blocks = NBTUtil.getByteArray(nbt, "blocks").getOrElse(new DefaultArray[Byte](16*16*16, 0))
    val meta = NBTUtil.getByteArray(nbt, "metadata").getOrElse(new DefaultArray[Byte](16*16*16, 0))

    for (i <- blockTypes.indices) {
      blockTypes(i) = blocks(i)
      metadata(i) = meta(i)
      if (blockTypes(i) != 0) {
        _numBlocks += 1
//        chunk.requestBlockUpdate(BlockRelChunk(i, chunk.world))
      }
    }
  }

  def toNBT: Seq[Tag[_]] = {
    Seq(new ByteArrayTag("blocks", blockTypes),
        new ByteArrayTag("metadata", metadata))
  }
}

class SparseChunkStorage(chunk: Chunk) extends ChunkStorage {
  private val blocks = scala.collection.mutable.Map.empty[Short, BlockState]

  def blockType(coord: BlockRelChunk): Block = blocks.get(coord.value.toShort).map(_.blockType).getOrElse(Block.Air)
  def getBlock(coord: BlockRelChunk): Option[BlockState] = blocks.get(coord.value.toShort)
  def setBlock(block: BlockState): Unit = blocks(block.coords.getBlockRelChunk.value.toShort) = block
  def removeBlock(coords: BlockRelChunk): Unit = blocks -= coords.value.toShort
  def allBlocks: Seq[BlockState] = blocks.values.toSeq
  def numBlocks: Int = blocks.size
  def isDense: Boolean = false
  def toDense: DenseChunkStorage = {
    val dense = new DenseChunkStorage(chunk)
    for (b <- blocks) dense.setBlock(b._2)
    dense
  }
  def toSparse: SparseChunkStorage = this

  def fromNBT(nbt: CompoundTag): Unit = {
    val blocks = NBTUtil.getByteArray(nbt, "blocks").getOrElse(new DefaultArray[Byte](16*16*16, 0))
    val meta = NBTUtil.getByteArray(nbt, "metadata").getOrElse(new DefaultArray[Byte](16*16*16, 0))

    for (i <- blocks.indices) {
      if (blocks(i) != 0) {
        setBlock(new BlockState(BlockRelWorld(i, chunk.world.size), chunk.world, Block.byId(blocks(i)), meta(i)))
//        chunk.requestBlockUpdate(BlockRelChunk(i, chunk.world))
      }
    }
  }

  def toNBT: Seq[Tag[_]] = {
    val ids = Array.tabulate[Byte](16*16*16)(i => blocks.get(i.toShort).map(_.blockType.id).getOrElse(0))
    val meta = Array.tabulate[Byte](16*16*16)(i => blocks.get(i.toShort).map(_.metadata).getOrElse(0))
    Seq(new ByteArrayTag("blocks", ids),
        new ByteArrayTag("metadata", meta))
  }
}
