package hexagon.world.storage

import hexagon.world.coord.BlockRelChunk
import hexagon.block.Block
import org.jnbt.CompoundTag
import org.jnbt.Tag
import org.jnbt.ByteArrayTag
import hexagon.world.coord.BlockRelWorld
import hexagon.block.BlockState

trait ChunkStorage {
  def blockType(coord: BlockRelChunk): Block

  def getBlock(coord: BlockRelChunk): Option[BlockState]
  def setBlock(block: BlockState): Unit
  def removeBlock(coords: BlockRelChunk): Unit

  def allBlocks: Seq[BlockState]
  def numBlocks: Int

  def isDense: Boolean
  def toDense(): DenseChunkStorage
  def toSparse(): SparseChunkStorage
  
  def fromNBT(nbt: CompoundTag): Unit
  def toNBT: Seq[Tag]
}

class DenseChunkStorage(chunk: Chunk) extends ChunkStorage {
  private val blockTypes = new Array[Byte](16 * 16 * 16)
  private var _numBlocks = 0

  def blockType(coord: BlockRelChunk): Block = Block.byId(blockTypes(coord.value))
  def getBlock(coord: BlockRelChunk): Option[BlockState] = {
    val _type = blockTypes(coord.value)
    if (_type != 0) Some(new BlockState(coord.withChunk(chunk.coords), Block.byId(_type))) else None
  }
  def setBlock(block: BlockState): Unit = {
    val idx = block.coord.getBlockRelChunk.value
    if (blockTypes(idx) == 0) _numBlocks += 1
    blockTypes(idx) = block.blockType.id
    if (blockTypes(idx) == 0) _numBlocks -= 1
  }
  def removeBlock(coords: BlockRelChunk): Unit = {
    if (blockTypes(coords.value) != 0) _numBlocks -= 1
    blockTypes(coords.value) = 0
  }
  def allBlocks: Seq[BlockState] = blockTypes.indices.filter(i => blockTypes(i) != 0).map(i => 
    new BlockState(BlockRelChunk(i).withChunk(chunk.coords), Block.byId(blockTypes(i))))
  def numBlocks: Int = _numBlocks
  def isDense: Boolean = true
  def toDense(): DenseChunkStorage = this
  def toSparse(): SparseChunkStorage = {
    val sparse = new SparseChunkStorage(chunk)
    for (b <- allBlocks) sparse.setBlock(b)
    sparse
  }

  def fromNBT(nbt: CompoundTag): Unit = {
    val blocks = nbt.getValue.get("blocks").asInstanceOf[ByteArrayTag].getValue
    for (i <- 0 until blockTypes.size) {
      blockTypes(i) = blocks(i)
      if (blockTypes(i) != 0) _numBlocks += 1
    }
  }

  def toNBT: Seq[Tag] = {
    Seq(new ByteArrayTag("blocks", blockTypes))
  }
}

class SparseChunkStorage(chunk: Chunk) extends ChunkStorage {
  private val blocks = scala.collection.mutable.Map.empty[Short, BlockState]

  def blockType(coord: BlockRelChunk): Block = blocks.get(coord.value.toShort).map(_.blockType).getOrElse(Block.Air)
  def getBlock(coord: BlockRelChunk): Option[BlockState] = blocks.get(coord.value.toShort)
  def setBlock(block: BlockState): Unit = blocks(block.coord.getBlockRelChunk.value.toShort) = block
  def removeBlock(coords: BlockRelChunk): Unit = blocks -= coords.value.toShort
  def allBlocks: Seq[BlockState] = blocks.values.toSeq
  def numBlocks: Int = blocks.size
  def isDense: Boolean = false
  def toDense(): DenseChunkStorage = {
    val dense = new DenseChunkStorage(chunk)
    for (b <- blocks) dense.setBlock(b._2)
    dense
  }
  def toSparse(): SparseChunkStorage = this

  def fromNBT(nbt: CompoundTag): Unit = {
    val blocks = nbt.getValue.get("blocks").asInstanceOf[ByteArrayTag].getValue
    for (i <- 0 until blocks.size) {
      if (blocks(i) != 0) {
        setBlock(new BlockState(BlockRelWorld(i), Block.byId(blocks(i))))
      }
    }
  }

  def toNBT: Seq[Tag] = {
    val arr = Array.tabulate[Byte](16*16*16)(i => blocks.get(i.toShort).map(_.blockType.id).getOrElse(0))
    Seq(new ByteArrayTag("blocks", arr))
  }
}
