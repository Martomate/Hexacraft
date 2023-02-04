package com.martomate.hexacraft.world.chunk.storage

import com.martomate.hexacraft.world.block.{Block, BlockState}
import com.martomate.hexacraft.world.coord.integer.BlockRelChunk

import com.flowpowered.nbt.Tag

abstract class ChunkStorage {
  def blockType(coords: BlockRelChunk): Block

  def getBlock(coords: BlockRelChunk): BlockState
  def setBlock(coords: BlockRelChunk, block: BlockState): Unit
  def removeBlock(coords: BlockRelChunk): Unit

  def allBlocks: Array[LocalBlockState]
  def numBlocks: Int

  def isDense: Boolean

  def toNBT: Seq[Tag[_]]
}

case class LocalBlockState(coords: BlockRelChunk, block: BlockState)
