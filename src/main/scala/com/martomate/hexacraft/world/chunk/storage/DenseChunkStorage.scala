package com.martomate.hexacraft.world.chunk.storage

import com.flowpowered.nbt.{ByteArrayTag, CompoundTag, Tag}
import com.martomate.hexacraft.util.{ConstantSeq, CylinderSize, NBTUtil, SmartArray}
import com.martomate.hexacraft.world.block.{Block, BlockState}
import com.martomate.hexacraft.world.coord.integer.{BlockRelChunk, ChunkRelWorld}

class DenseChunkStorage(_chunkCoords: ChunkRelWorld)(using CylinderSize) extends ChunkStorage(_chunkCoords) {
  private val blockTypes = SmartArray.withByteArray(16 * 16 * 16, 0)
  private val metadata = SmartArray.withByteArray(16 * 16 * 16, 0)
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
  def allBlocks: Array[LocalBlockState] = {
    val arr = Array.ofDim[LocalBlockState](_numBlocks)
    var idx = 0
    for (i <- blockTypes.indices) {
      if (blockTypes(i) != 0) {
        arr(idx) = LocalBlockState(BlockRelChunk(i), new BlockState(Block.byId(blockTypes(i)), metadata(i)))
        idx += 1
      }
    }
    arr
  }

  def numBlocks: Int = _numBlocks
  def isDense: Boolean = true

  def toNBT: Seq[Tag[_]] = {
    Seq(
      new ByteArrayTag("blocks", blockTypes.toArray),
      new ByteArrayTag("metadata", metadata.toArray)
    )
  }
}

object DenseChunkStorage:
  def fromStorage(storage: ChunkStorage)(using CylinderSize): DenseChunkStorage =
    val result = new DenseChunkStorage(storage.chunkCoords)
    for LocalBlockState(i, b) <- storage.allBlocks do result.setBlock(i, b)
    result

  def fromNBT(nbt: CompoundTag)(coords: ChunkRelWorld)(using CylinderSize): DenseChunkStorage =
    val storage = new DenseChunkStorage(coords)

    val blocks =
      NBTUtil.getByteArray(nbt, "blocks").getOrElse(new ConstantSeq[Byte](16 * 16 * 16, 0))
    val meta =
      NBTUtil.getByteArray(nbt, "metadata").getOrElse(new ConstantSeq[Byte](16 * 16 * 16, 0))

    for i <- storage.blockTypes.indices do
      storage.blockTypes(i) = blocks(i)
      storage.metadata(i) = meta(i)
      if storage.blockTypes(i) != 0 then storage._numBlocks += 1

    storage
