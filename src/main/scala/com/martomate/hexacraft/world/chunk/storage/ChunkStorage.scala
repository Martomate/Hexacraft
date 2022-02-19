package com.martomate.hexacraft.world.chunk.storage

import com.flowpowered.nbt.{CompoundTag, Tag}
import com.martomate.hexacraft.world.block.Block
import com.martomate.hexacraft.world.block.state.BlockState
import com.martomate.hexacraft.world.chunk.BlockInChunkAccessor
import com.martomate.hexacraft.world.coord.integer.{BlockRelChunk, ChunkRelWorld}

abstract class ChunkStorage(val chunkCoords: ChunkRelWorld) extends BlockInChunkAccessor {
  def blockType(coords: BlockRelChunk): Block

  def getBlock(coords: BlockRelChunk): BlockState
  def setBlock(coords: BlockRelChunk, block: BlockState): Unit
  def removeBlock(coords: BlockRelChunk): Unit

  def allBlocks: Array[LocalBlockState]
  def numBlocks: Int

  def isDense: Boolean

  def fromNBT(nbt: CompoundTag): Unit
  def toNBT: Seq[Tag[_]]
}

case class LocalBlockState(coords: BlockRelChunk, block: BlockState)
