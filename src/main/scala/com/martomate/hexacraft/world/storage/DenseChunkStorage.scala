package com.martomate.hexacraft.world.storage

import com.flowpowered.nbt.{ByteArrayTag, CompoundTag, Tag}
import com.martomate.hexacraft.util.{ConstantSeq, NBTUtil, SmartArray}
import com.martomate.hexacraft.world.block.{Block, Blocks}
import com.martomate.hexacraft.world.block.state.BlockState
import com.martomate.hexacraft.world.coord.integer.{BlockRelChunk, ChunkRelWorld}

import scala.collection.mutable.ArrayBuffer

class DenseChunkStorage(_chunkCoords: ChunkRelWorld) extends ChunkStorage(_chunkCoords) {
  def this(storage: ChunkStorage) = {
    this(storage.chunkCoords)
    for ((i, b) <- storage.allBlocks) setBlock(i, b)
  }

  private val blockTypes = SmartArray.withByteArray(16*16*16, 0)
  private val metadata = SmartArray.withByteArray(16*16*16, 0)
  private var _numBlocks = 0

  def blockType(coords: BlockRelChunk): Block = Block.byId(blockTypes(coords.value))
  def getBlock(coords: BlockRelChunk): BlockState = {
    val _type = blockTypes(coords.value)
    if (_type != 0) new BlockState(Block.byId(_type), metadata(coords.value))
    else BlockState.Air
  }
  def mapBlock[T](coords: BlockRelChunk, func: (Block, Byte) => T): T = {
    func(blockType(coords), metadata(coords.value))
  }
  def setBlock(coords: BlockRelChunk, block: BlockState): Unit = {
    val idx = coords.value
    if (blockTypes(idx) == 0) _numBlocks += 1
    blockTypes(idx) = block.blockType.id
    metadata(idx) = block.metadata
    if (blockTypes(idx) == 0) _numBlocks -= 1
  }
  def removeBlock(coords: BlockRelChunk): Unit = {
    if (blockTypes(coords.value) != 0) _numBlocks -= 1
    blockTypes(coords.value) = 0
  }
  def allBlocks: Seq[(BlockRelChunk, BlockState)] = {
    val buffer = new ArrayBuffer[(BlockRelChunk, BlockState)](numBlocks)
    for (i <- blockTypes.indices) {
      if (blockTypes(i) != 0)
        buffer += ((BlockRelChunk(i)(chunkCoords.cylSize), new BlockState(Block.byId(blockTypes(i)), metadata(i))))
    }
    buffer
  }

  def numBlocks: Int = _numBlocks
  def isDense: Boolean = true

  def fromNBT(nbt: CompoundTag): Unit = {
    val blocks = NBTUtil.getByteArray(nbt, "blocks").getOrElse(new ConstantSeq[Byte](16*16*16, 0))
    val meta = NBTUtil.getByteArray(nbt, "metadata").getOrElse(new ConstantSeq[Byte](16*16*16, 0))

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
    Seq(new ByteArrayTag("blocks", blockTypes.toArray),
        new ByteArrayTag("metadata", metadata.toArray))
  }
}
