package com.martomate.hexacraft.world.chunk.storage

import com.flowpowered.nbt.{ByteArrayTag, CompoundTag, Tag}
import com.martomate.hexacraft.nbt.{Nbt, NBTUtil}
import com.martomate.hexacraft.util.SmartArray
import com.martomate.hexacraft.world.CylinderSize
import com.martomate.hexacraft.world.block.{Block, Blocks, BlockState}
import com.martomate.hexacraft.world.coord.integer.{BlockRelChunk, ChunkRelWorld}

class DenseChunkStorage extends ChunkStorage:
  private val blockTypes = SmartArray.withByteArray(16 * 16 * 16, 0)
  private val metadata = SmartArray.withByteArray(16 * 16 * 16, 0)
  private var _numBlocks = 0

  def blockType(coords: BlockRelChunk): Block = Block.byId(blockTypes(coords.value))

  def getBlock(coords: BlockRelChunk): BlockState =
    val _type = blockTypes(coords.value)
    if _type != 0 then new BlockState(Block.byId(_type), metadata(coords.value))
    else BlockState.Air

  def setBlock(coords: BlockRelChunk, block: BlockState): Unit =
    val idx = coords.value
    if blockTypes(idx) == 0 then _numBlocks += 1
    blockTypes(idx) = block.blockType.id
    metadata(idx) = block.metadata
    if blockTypes(idx) == 0 then _numBlocks -= 1

  def removeBlock(coords: BlockRelChunk): Unit =
    if blockTypes(coords.value) != 0 then _numBlocks -= 1
    blockTypes(coords.value) = 0

  def allBlocks: Array[LocalBlockState] =
    val arr = Array.ofDim[LocalBlockState](_numBlocks)
    var idx = 0
    for i <- blockTypes.indices do
      if blockTypes(i) != 0 then
        arr(idx) = LocalBlockState(BlockRelChunk(i), new BlockState(Block.byId(blockTypes(i)), metadata(i)))
        idx += 1
    arr

  def numBlocks: Int = _numBlocks
  def isDense: Boolean = true

  def toNBT: ChunkStorage.NbtData =
    ChunkStorage.NbtData(blocks = blockTypes.toArray, metadata = metadata.toArray)

object DenseChunkStorage:
  def fromStorage(storage: ChunkStorage): DenseChunkStorage =
    val result = new DenseChunkStorage
    for LocalBlockState(i, b) <- storage.allBlocks do result.setBlock(i, b)
    result

  def fromNBT(blocks: Array[Byte], metadata: Option[Array[Byte]])(using
      CylinderSize,
      Blocks
  ): DenseChunkStorage =
    val storage = new DenseChunkStorage

    val meta = metadata.map(_.apply).getOrElse(_ => 0)

    for i <- storage.blockTypes.indices do
      storage.blockTypes(i) = blocks(i)
      storage.metadata(i) = meta(i)
      if storage.blockTypes(i) != 0 then storage._numBlocks += 1

    storage
