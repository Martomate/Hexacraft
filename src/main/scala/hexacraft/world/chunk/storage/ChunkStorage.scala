package hexacraft.world.chunk.storage

import com.flowpowered.nbt.Tag
import hexacraft.world.block.{Block, BlockState}
import hexacraft.world.coord.integer.BlockRelChunk

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
