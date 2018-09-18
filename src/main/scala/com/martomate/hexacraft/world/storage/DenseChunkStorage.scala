package com.martomate.hexacraft.world.storage

import com.flowpowered.nbt.{ByteArrayTag, CompoundTag, Tag}
import com.martomate.hexacraft.util.{ConstantSeq, NBTUtil}
import com.martomate.hexacraft.world.block.state.BlockState
import com.martomate.hexacraft.world.block.{Block, BlockAir}
import com.martomate.hexacraft.world.coord.integer.{BlockRelChunk, ChunkRelWorld}

class DenseChunkStorage(val chunkCoords: ChunkRelWorld) extends ChunkStorage {
  def this(storage: ChunkStorage) = {
    this(storage.chunkCoords)
    for ((i, b) <- storage.allBlocks) setBlock(i, b)
  }
  private val blockTypes = new Array[Byte](16 * 16 * 16)
  private val metadata = new Array[Byte](16 * 16 * 16)
  private var _numBlocks = 0

  def blockType(coords: BlockRelChunk): Block = Block.byId(blockTypes(coords.value))
  def getBlock(coords: BlockRelChunk): BlockState = {
    val _type = blockTypes(coords.value)
    if (_type != 0) new BlockState(Block.byId(_type), metadata(coords.value)) else BlockState.Air
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
  def allBlocks: Seq[(BlockRelChunk, BlockState)] = blockTypes.indices.filter(i => blockTypes(i) != 0).map(i =>
    (BlockRelChunk(i, chunkCoords.cylSize), new BlockState(Block.byId(blockTypes(i)), metadata(i))))
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
    Seq(new ByteArrayTag("blocks", blockTypes),
        new ByteArrayTag("metadata", metadata))
  }
}
