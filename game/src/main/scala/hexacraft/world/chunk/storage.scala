package hexacraft.world.chunk

import hexacraft.util.SmartArray
import hexacraft.world.block.{Block, BlockState}
import hexacraft.world.coord.BlockRelChunk

import scala.collection.mutable

object ChunkStorage {
  case class NbtData(blocks: Array[Byte], metadata: Array[Byte])
}

abstract class ChunkStorage {
  def blockType(coords: BlockRelChunk): Block

  def getBlock(coords: BlockRelChunk): BlockState
  def setBlock(coords: BlockRelChunk, block: BlockState): Unit
  def removeBlock(coords: BlockRelChunk): Unit

  def allBlocks: Array[LocalBlockState]
  def numBlocks: Int

  def isDense: Boolean

  def toNBT: ChunkStorage.NbtData
}

case class LocalBlockState(coords: BlockRelChunk, block: BlockState)

class DenseChunkStorage extends ChunkStorage {
  private val blockTypes = SmartArray.withByteArray(16 * 16 * 16, 0)
  private val metadata = SmartArray.withByteArray(16 * 16 * 16, 0)
  private var _numBlocks = 0

  def blockType(coords: BlockRelChunk): Block = Block.byId(blockTypes(coords.value))

  def getBlock(coords: BlockRelChunk): BlockState = {
    val _type = blockTypes(coords.value)
    if _type != 0 then {
      new BlockState(Block.byId(_type), metadata(coords.value))
    } else {
      BlockState.Air
    }
  }

  def setBlock(coords: BlockRelChunk, block: BlockState): Unit = {
    val idx = coords.value
    if blockTypes(idx) == 0 then {
      _numBlocks += 1
    }
    blockTypes(idx) = block.blockType.id
    metadata(idx) = block.metadata
    if blockTypes(idx) == 0 then {
      _numBlocks -= 1
    }
  }

  def removeBlock(coords: BlockRelChunk): Unit = {
    if blockTypes(coords.value) != 0 then {
      _numBlocks -= 1
    }
    blockTypes(coords.value) = 0
  }

  def allBlocks: Array[LocalBlockState] = {
    val arr = Array.ofDim[LocalBlockState](_numBlocks)
    var idx = 0
    for i <- blockTypes.indices do {
      if blockTypes(i) != 0 then {
        arr(idx) = LocalBlockState(BlockRelChunk(i), new BlockState(Block.byId(blockTypes(i)), metadata(i)))
        idx += 1
      }
    }
    arr
  }

  def numBlocks: Int = _numBlocks

  def isDense: Boolean = true

  def toNBT: ChunkStorage.NbtData = {
    ChunkStorage.NbtData(blocks = blockTypes.toArray, metadata = metadata.toArray)
  }
}

object DenseChunkStorage {
  def fromStorage(storage: ChunkStorage): DenseChunkStorage = {
    val result = new DenseChunkStorage
    for LocalBlockState(i, b) <- storage.allBlocks do result.setBlock(i, b)
    result
  }

  def fromNBT(blocks: Array[Byte], metadata: Option[Array[Byte]]): DenseChunkStorage = {
    val storage = new DenseChunkStorage

    val meta = metadata.map(_.apply).getOrElse(_ => 0)

    for i <- storage.blockTypes.indices do {
      storage.blockTypes(i) = blocks(i)
      storage.metadata(i) = meta(i)
      if storage.blockTypes(i) != 0 then {
        storage._numBlocks += 1
      }
    }

    storage
  }
}

class SparseChunkStorage extends ChunkStorage {
  private val blocks = mutable.LongMap.withDefault[BlockState](_ => BlockState.Air)

  def blockType(coords: BlockRelChunk): Block = blocks(coords.value.toShort).blockType

  def getBlock(coords: BlockRelChunk): BlockState = blocks(coords.value.toShort)

  def setBlock(coords: BlockRelChunk, block: BlockState): Unit = {
    if block.blockType != Block.Air then {
      blocks(coords.value) = block
    } else {
      removeBlock(coords)
    }
  }

  def removeBlock(coords: BlockRelChunk): Unit = {
    blocks -= coords.value
  }

  def allBlocks: Array[LocalBlockState] = {
    blocks
      .map(t => LocalBlockState(BlockRelChunk(t._1.toInt), t._2))
      .toArray[LocalBlockState]
  }

  def numBlocks: Int = blocks.size

  def isDense: Boolean = false

  def toNBT: ChunkStorage.NbtData = {
    val ids = Array.tabulate[Byte](16 * 16 * 16)(i => blocks.get(i.toShort).map(_.blockType.id).getOrElse(0))
    val meta = Array.tabulate[Byte](16 * 16 * 16)(i => blocks.get(i.toShort).map(_.metadata).getOrElse(0))
    ChunkStorage.NbtData(blocks = ids, metadata = meta)
  }
}

object SparseChunkStorage {
  def empty: ChunkStorage = new SparseChunkStorage

  def fromStorage(storage: ChunkStorage): SparseChunkStorage = {
    val result = new SparseChunkStorage
    for LocalBlockState(i, b) <- storage.allBlocks do result.setBlock(i, b)
    result
  }
}
