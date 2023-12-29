package hexacraft.world

import hexacraft.world.block.BlockState
import hexacraft.world.chunk.{Chunk, ChunkColumn, ChunkColumnData, ChunkColumnHeightMap}
import hexacraft.world.coord.{BlockRelWorld, ChunkRelWorld, ColumnRelWorld}

import scala.collection.mutable

class FakeBlocksInWorld private (provider: FakeWorldProvider)(using CylinderSize) extends BlocksInWorld {
  private val worldGenerator = new WorldGenerator(provider.getWorldInfo.gen)
  private var cols: Map[ColumnRelWorld, ChunkColumn] = Map.empty

  override def getColumn(coords: ColumnRelWorld): Option[ChunkColumn] =
    cols.get(coords)

  override def getChunk(coords: ChunkRelWorld): Option[Chunk] =
    getColumn(coords.getColumnRelWorld).flatMap(_.getChunk(coords.Y))

  override def getBlock(coords: BlockRelWorld): BlockState =
    getChunk(coords.getChunkRelWorld)
      .map(_.getBlock(coords.getBlockRelChunk))
      .getOrElse(BlockState.Air)

  override def provideColumn(coords: ColumnRelWorld): ChunkColumn =
    if cols.contains(coords)
    then cols(coords)
    else
      val col = ChunkColumn.create(
        coords,
        ChunkColumnHeightMap.fromData2D(worldGenerator.getHeightmapInterpolator(coords)),
        provider.loadColumnData(coords).map(ChunkColumnData.fromNbt)
      )
      cols += coords -> col
      col

  override def toString: String =
    val sb = new mutable.StringBuilder
    for col <- cols.values do
      for ch <- col.allChunks do
        val blocksStr = ch.blocks.map(s => s"${s.coords} -> ${s.block.blockType.displayName}").mkString(", ")
        sb.append(ch.coords).append(": ").append(blocksStr).append("\n")
    sb.toString
}

object FakeBlocksInWorld {
  def empty(provider: FakeWorldProvider)(using CylinderSize): FakeBlocksInWorld =
    new FakeBlocksInWorld(provider)

  def withBlocks(provider: FakeWorldProvider, blocks: Map[BlockRelWorld, BlockState])(using
      CylinderSize
  ): FakeBlocksInWorld =
    val world = new FakeBlocksInWorld(provider)
    for coords -> block <- blocks do
      val col = world.provideColumn(coords.getColumnRelWorld)
      val chunk = col.getChunk(coords.Y) match
        case Some(c) => c
        case None =>
          val c = Chunk.fromGenerator(coords.getChunkRelWorld, world, WorldGenerator(provider.getWorldInfo.gen))
          col.setChunk(c)
          c

      chunk.setBlock(coords.getBlockRelChunk, block)
    world
}
