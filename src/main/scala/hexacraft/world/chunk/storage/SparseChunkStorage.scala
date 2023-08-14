package hexacraft.world.chunk.storage

import hexacraft.world.CylinderSize
import hexacraft.world.block.{Block, BlockState}
import hexacraft.world.coord.integer.BlockRelChunk

import scala.collection.mutable

class SparseChunkStorage extends ChunkStorage:
  private val blocks = mutable.LongMap.withDefault[BlockState](_ => BlockState.Air)

  def blockType(coords: BlockRelChunk): Block = blocks(coords.value.toShort).blockType

  def getBlock(coords: BlockRelChunk): BlockState = blocks(coords.value.toShort)

  def setBlock(coords: BlockRelChunk, block: BlockState): Unit =
    if block.blockType != Block.Air
    then blocks(coords.value) = block
    else removeBlock(coords)

  def removeBlock(coords: BlockRelChunk): Unit = blocks -= coords.value

  def allBlocks: Array[LocalBlockState] = blocks
    .map(t => LocalBlockState(BlockRelChunk(t._1.toInt), t._2))
    .toArray[LocalBlockState]

  def numBlocks: Int = blocks.size

  def isDense: Boolean = false

  def toNBT: ChunkStorage.NbtData =
    val ids = Array.tabulate[Byte](16 * 16 * 16)(i => blocks.get(i.toShort).map(_.blockType.id).getOrElse(0))
    val meta = Array.tabulate[Byte](16 * 16 * 16)(i => blocks.get(i.toShort).map(_.metadata).getOrElse(0))
    ChunkStorage.NbtData(blocks = ids, metadata = meta)

object SparseChunkStorage:
  def empty(using CylinderSize): ChunkStorage = new SparseChunkStorage

  def fromStorage(storage: ChunkStorage): SparseChunkStorage =
    val result = new SparseChunkStorage
    for LocalBlockState(i, b) <- storage.allBlocks do result.setBlock(i, b)
    result
