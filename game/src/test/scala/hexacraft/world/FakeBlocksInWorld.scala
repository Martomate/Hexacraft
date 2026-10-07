package hexacraft.world

import hexacraft.world.block.{Block, BlockState}
import hexacraft.world.chunk.{Chunk, ChunkColumnHeightMap, DenseChunkStorage}
import hexacraft.world.coord.{BlockRelWorld, ChunkRelWorld, ColumnRelWorld}

import scala.collection.mutable

/** A world containing only the blocks placed in it. Chunks are created on demand and are filled with air. */
class FakeBlocksInWorld private (using CylinderSize) extends BlocksInWorld {
  private var cols: Map[ColumnRelWorld, ChunkColumnHeightMap] = Map.empty
  private var chunks: Map[ChunkRelWorld, Chunk] = Map.empty

  override def getColumn(coords: ColumnRelWorld): Option[ChunkColumnHeightMap] = {
    cols.get(coords)
  }

  override def getChunk(coords: ChunkRelWorld): Option[Chunk] = {
    chunks.get(coords)
  }

  override def getBlock(coords: BlockRelWorld): BlockState = {
    getChunk(coords.getChunkRelWorld)
      .map(_.getBlock(coords.getBlockRelChunk))
      .getOrElse(BlockState.Air)
  }

  /** Returns the column, creating an empty one (with no blocks in it) if needed */
  def provideColumn(coords: ColumnRelWorld): ChunkColumnHeightMap = {
    cols.get(coords) match {
      case Some(col) => col
      case None =>
        val col = ChunkColumnHeightMap.from((_, _) => Short.MinValue)
        cols += coords -> col
        col
    }
  }

  /** Places a block, creating an empty chunk if needed, and updates the height map.
    * Removing a block (by setting it to air) is not supported since it would require more advanced heightmap logic.
    */
  def addBlock(coords: BlockRelWorld, block: BlockState): Unit = {
    val col = provideColumn(coords.getColumnRelWorld)

    val chunkCoords = coords.getChunkRelWorld
    val chunk = chunks.get(chunkCoords) match {
      case Some(c) => c
      case None =>
        val ch = Chunk.from(new DenseChunkStorage)
        chunks += chunkCoords -> ch
        ch
    }

    val currentBlock = chunk.getBlock(coords.getBlockRelChunk)
    if block.blockType == Block.Air && currentBlock.blockType != Block.Air then {
      throw new IllegalArgumentException("removing blocks is not supported because height map could become wrong")
    }

    chunk.setBlock(coords.getBlockRelChunk, block)

    if block.blockType != Block.Air && coords.y > col.getHeight(coords.cx, coords.cz) then {
      col.setHeight(coords.cx, coords.cz, coords.y.toShort)
    }
  }

  def removeChunk(coords: ChunkRelWorld): Unit = {
    chunks -= coords
  }

  def setChunk(coords: ChunkRelWorld, chunk: Chunk): Unit = {
    provideColumn(coords.getColumnRelWorld)
    chunks += coords -> chunk
  }

  override def toString: String = {
    val sb = new mutable.StringBuilder
    for (cCoords, ch) <- chunks do {
      val blocksStr = ch.blocks.map(s => s"${s.coords} -> ${s.block.blockType.displayName}").mkString(", ")
      sb.append(cCoords).append(": ").append(blocksStr).append("\n")
    }
    sb.toString
  }
}

object FakeBlocksInWorld {
  def empty(using CylinderSize): FakeBlocksInWorld = {
    new FakeBlocksInWorld
  }

  /** Creates a world where the chunks containing `blocks` are loaded. All other blocks in those chunks are air. */
  def withBlocks(blocks: Map[BlockRelWorld, BlockState])(using CylinderSize): FakeBlocksInWorld = {
    val world = new FakeBlocksInWorld
    for coords -> block <- blocks do {
      world.addBlock(coords, block)
    }
    world
  }
}
