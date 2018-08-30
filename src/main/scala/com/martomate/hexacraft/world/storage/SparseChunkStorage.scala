package com.martomate.hexacraft.world.storage

import com.flowpowered.nbt.{ByteArrayTag, CompoundTag, Tag}
import com.martomate.hexacraft.block.{Block, BlockAir, BlockState}
import com.martomate.hexacraft.util.{ConstantSeq, NBTUtil}
import com.martomate.hexacraft.world.coord.{BlockRelChunk, ChunkRelWorld}

class SparseChunkStorage(chunkCoords: ChunkRelWorld) extends ChunkStorage {
  private val blocks = scala.collection.mutable.Map.empty[Short, BlockState]

  def blockType(coords: BlockRelChunk): Block = blocks.get(coords.value.toShort).map(_.blockType).getOrElse(Block.Air)
  def getBlock(coords: BlockRelChunk): BlockState = blocks.getOrElse(coords.value.toShort, BlockAir.State)
  def setBlock(coords: BlockRelChunk, block: BlockState): Unit = {
    if (block.blockType != Block.Air) blocks(coords.value.toShort) = block
    else removeBlock(coords)
  }
  def removeBlock(coords: BlockRelChunk): Unit = blocks -= coords.value.toShort
  def allBlocks: Seq[(BlockRelChunk, BlockState)] = blocks.toSeq.map(t => (BlockRelChunk(t._1, chunkCoords.cylSize), t._2))
  def numBlocks: Int = blocks.size
  def isDense: Boolean = false
  def toDense: DenseChunkStorage = {
    val dense = new DenseChunkStorage(chunkCoords)
    for ((i, b) <- blocks) dense.setBlock(BlockRelChunk(i, chunkCoords.cylSize), b)
    dense
  }
  def toSparse: SparseChunkStorage = this

  def fromNBT(nbt: CompoundTag): Unit = {
    val blocks = NBTUtil.getByteArray(nbt, "blocks").getOrElse(new ConstantSeq[Byte](16*16*16, 0))
    val meta = NBTUtil.getByteArray(nbt, "metadata").getOrElse(new ConstantSeq[Byte](16*16*16, 0))

    for (i <- blocks.indices) {
      if (blocks(i) != 0) {
        setBlock(BlockRelChunk(i, chunkCoords.cylSize), new BlockState(Block.byId(blocks(i)), meta(i)))
//        chunk.requestBlockUpdate(BlockRelChunk(i, chunk.world))
      }
    }
  }

  def toNBT: Seq[Tag[_]] = {
    val ids = Array.tabulate[Byte](16*16*16)(i => blocks.get(i.toShort).map(_.blockType.id).getOrElse(0))
    val meta = Array.tabulate[Byte](16*16*16)(i => blocks.get(i.toShort).map(_.metadata).getOrElse(0))
    Seq(new ByteArrayTag("blocks", ids),
        new ByteArrayTag("metadata", meta))
  }
}
