package com.martomate.hexacraft.world.storage

import com.flowpowered.nbt.{CompoundTag, Tag}
import com.martomate.hexacraft.block.{Block, BlockState}
import com.martomate.hexacraft.world.coord.integer.{BlockRelChunk, ChunkRelWorld}

trait ChunkStorage {
  def chunkCoords: ChunkRelWorld

  def blockType(coords: BlockRelChunk): Block

  def getBlock(coords: BlockRelChunk): BlockState
  def setBlock(coords: BlockRelChunk, block: BlockState): Unit
  def removeBlock(coords: BlockRelChunk): Unit

  def allBlocks: Seq[(BlockRelChunk, BlockState)]
  def numBlocks: Int

  def isDense: Boolean

  def fromNBT(nbt: CompoundTag): Unit
  def toNBT: Seq[Tag[_]]
}

