package com.martomate.hexacraft.world

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.state.BlockState
import com.martomate.hexacraft.world.chunk.Chunk
import com.martomate.hexacraft.world.column.ChunkColumn
import com.martomate.hexacraft.world.coord.integer.{BlockRelWorld, ChunkRelWorld, ColumnRelWorld}
import com.martomate.hexacraft.world.gen.WorldGenerator

import java.util

class FakeBlocksInWorld private(provider: FakeWorldProvider)(implicit cylSize: CylinderSize) extends BlocksInWorld {
  private val worldGenerator = new WorldGenerator(provider.getWorldInfo.gen)
  private var cols: Map[ColumnRelWorld, ChunkColumn] = Map.empty

  override def getColumn(coords: ColumnRelWorld): Option[ChunkColumn] =
    cols.get(coords)

  override def getChunk(coords: ChunkRelWorld): Option[Chunk] =
    getColumn(coords.getColumnRelWorld)
      .flatMap(_.getChunk(coords.getChunkRelColumn))

  override def getBlock(coords: BlockRelWorld): BlockState =
    getChunk(coords.getChunkRelWorld)
      .map(_.getBlock(coords.getBlockRelChunk))
      .getOrElse(BlockState.Air)

  override def provideColumn(coords: ColumnRelWorld): ChunkColumn = {
    if (cols.contains(coords)) cols(coords)
    else {
      val col = new ChunkColumn(coords, worldGenerator, provider)
      cols += coords -> col
      col
    }
  }

  override def toString: String = {
    val sb = new StringBuilder
    for (col <- cols.values) {
      for (ch <- col.allChunks) {
        val blocksStr = Seq(ch.blocks.allBlocks: _*).map(s => s"${s.coords} -> ${s.block.blockType.displayName}").mkString(", ")
        sb.append(ch.coords).append(": ").append(blocksStr).append("\n")
      }
    }
    sb.toString
  }
}

object FakeBlocksInWorld {
  def empty(provider: FakeWorldProvider)(implicit cylSize: CylinderSize): FakeBlocksInWorld =
    new FakeBlocksInWorld(provider)

  def withBlocks(provider: FakeWorldProvider, blocks: Map[BlockRelWorld, BlockState])(implicit cylSize: CylinderSize): FakeBlocksInWorld = {
    val world = new FakeBlocksInWorld(provider)
    for (coords -> block <- blocks) {
      val col = world.provideColumn(coords.getColumnRelWorld)
      val chunk = col.getChunk(coords.getChunkRelColumn) match {
        case Some(c) => c
        case None =>
          val c = Chunk(coords.getChunkRelWorld, world, provider)
          col.setChunk(c)
          c
      }
      chunk.setBlock(coords.getBlockRelChunk, block)
    }
    world
  }
}
