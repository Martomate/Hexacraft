package hexacraft.world

import hexacraft.world.block.BlockState
import hexacraft.world.chunk.{Chunk, ChunkColumn, ChunkColumnData, ChunkColumnHeightMap, ChunkColumnTerrain}
import hexacraft.world.coord.{BlockRelWorld, ChunkRelWorld, ColumnRelWorld}

import scala.collection.mutable

class FakeBlocksInWorld private (provider: FakeWorldProvider)(using CylinderSize) extends BlocksInWorld {
  private val worldGenerator = new WorldGenerator(provider.getWorldInfo.gen)
  private var cols: Map[ColumnRelWorld, ChunkColumn] = Map.empty
  private var chunks: Map[ChunkRelWorld, Chunk] = Map.empty

  override def getColumn(coords: ColumnRelWorld): Option[ChunkColumnTerrain] = {
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

  override def provideColumn(coords: ColumnRelWorld): ChunkColumnTerrain = {
    if cols.contains(coords) then {
      cols(coords)
    } else {
      val col = ChunkColumn.create(
        coords,
        ChunkColumnHeightMap.fromData2D(worldGenerator.getHeightmapInterpolator(coords)),
        provider.loadColumnData(coords).map(ChunkColumnData.fromNbt)
      )
      cols += coords -> col
      col
    }
  }

  def removeChunk(coords: ChunkRelWorld): Unit = {
    chunks -= coords
  }

  override def toString: String = {
    val sb = new mutable.StringBuilder
    for ch <- chunks.values do {
      val blocksStr = ch.blocks.map(s => s"${s.coords} -> ${s.block.blockType.displayName}").mkString(", ")
      sb.append(ch.coords).append(": ").append(blocksStr).append("\n")
    }
    sb.toString
  }
}

object FakeBlocksInWorld {
  def empty(provider: FakeWorldProvider)(using CylinderSize): FakeBlocksInWorld = {
    new FakeBlocksInWorld(provider)
  }

  def withBlocks(provider: FakeWorldProvider, blocks: Map[BlockRelWorld, BlockState])(using
      CylinderSize
  ): FakeBlocksInWorld = {
    val world = new FakeBlocksInWorld(provider)
    for coords -> block <- blocks do {
      world.provideColumn(coords.getColumnRelWorld)

      val chunkCoords = coords.getChunkRelWorld
      val chunk = world.chunks.get(chunkCoords) match {
        case Some(c) => c
        case None =>
          val ch = Chunk.fromGenerator(coords.getChunkRelWorld, world, WorldGenerator(provider.getWorldInfo.gen))
          world.chunks += chunkCoords -> ch
          ch
      }
      chunk.setBlock(coords.getBlockRelChunk, block)
    }
    world
  }
}
