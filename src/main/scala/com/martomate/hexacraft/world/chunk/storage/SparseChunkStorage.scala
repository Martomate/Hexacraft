package com.martomate.hexacraft.world.chunk.storage

import com.martomate.hexacraft.util.{CylinderSize, NBTUtil}
import com.martomate.hexacraft.world.block.{Block, Blocks, BlockState}
import com.martomate.hexacraft.world.coord.integer.{BlockRelChunk, ChunkRelWorld}

import com.flowpowered.nbt.{ByteArrayTag, CompoundTag, Tag}
import scala.collection.mutable

class SparseChunkStorage(using Blocks: Blocks) extends ChunkStorage:
  private val blocks = mutable.LongMap.withDefault[BlockState](_ => BlockState.Air)

  def blockType(coords: BlockRelChunk): Block = blocks(coords.value.toShort).blockType

  def getBlock(coords: BlockRelChunk): BlockState = blocks(coords.value.toShort)

  def setBlock(coords: BlockRelChunk, block: BlockState): Unit =
    if block.blockType != Blocks.Air
    then blocks(coords.value) = block
    else removeBlock(coords)

  def removeBlock(coords: BlockRelChunk): Unit = blocks -= coords.value

  def allBlocks: Array[LocalBlockState] = blocks
    .map(t => LocalBlockState(BlockRelChunk(t._1.toInt), t._2))
    .toArray[LocalBlockState]

  def numBlocks: Int = blocks.size

  def isDense: Boolean = false

  def toNBT: Seq[Tag[_]] =
    val ids = Array.tabulate[Byte](16 * 16 * 16)(i => blocks.get(i.toShort).map(_.blockType.id).getOrElse(0))
    val meta = Array.tabulate[Byte](16 * 16 * 16)(i => blocks.get(i.toShort).map(_.metadata).getOrElse(0))
    Seq(new ByteArrayTag("blocks", ids), new ByteArrayTag("metadata", meta))

object SparseChunkStorage extends ChunkStorageFactory:
  override def empty(using CylinderSize, Blocks): ChunkStorage = new SparseChunkStorage

  def fromStorage(storage: ChunkStorage)(using Blocks): SparseChunkStorage =
    val result = new SparseChunkStorage
    for LocalBlockState(i, b) <- storage.allBlocks do result.setBlock(i, b)
    result

  override def fromNBT(nbt: CompoundTag)(using CylinderSize, Blocks): SparseChunkStorage =
    val result = new SparseChunkStorage

    NBTUtil.getByteArray(nbt, "blocks") match
      case Some(blocks) =>
        val meta: Int => Byte = NBTUtil.getByteArray(nbt, "metadata").map(_.apply).getOrElse(_ => 0)

        for
          i <- blocks.indices
          if blocks(i) != 0
        do result.setBlock(BlockRelChunk(i), new BlockState(Block.byId(blocks(i)), meta(i)))
      case None =>

    result
