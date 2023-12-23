package hexacraft.world.chunk

import hexacraft.world.{BlocksInWorld, CylinderSize, WorldGenerator}
import hexacraft.world.block.{Block, BlockState}
import hexacraft.world.chunk.storage.{ChunkStorage, DenseChunkStorage}
import hexacraft.world.coord.integer.{BlockRelChunk, ChunkRelWorld}

class ChunkGenerator(coords: ChunkRelWorld, world: BlocksInWorld, worldGenerator: WorldGenerator)(using CylinderSize) {
  def generate(): ChunkData =
    val storage: ChunkStorage = new DenseChunkStorage
    val column = world.provideColumn(coords.getColumnRelWorld)
    val blockNoise = worldGenerator.getBlockInterpolator(coords)

    for (i <- 0 until 16; j <- 0 until 16; k <- 0 until 16) {
      val noise = blockNoise(i, j, k)
      val yToGo = coords.Y.toInt * 16 + j - column.originalTerrainHeight(i, k)
      val limit = limitForBlockNoise(yToGo)
      if (noise > limit)
        storage.setBlock(BlockRelChunk(i, j, k), new BlockState(getBlockAtDepth(yToGo)))
    }

    val data = ChunkData.fromStorage(storage)
    data

  private def getBlockAtDepth(yToGo: Int) = {
    if (yToGo < -5) Block.Stone
    else if (yToGo < -1) Block.Dirt
    else Block.Grass
  }

  private def limitForBlockNoise(yToGo: Int): Double = {
    if (yToGo < -6) -0.4
    else if (yToGo < 0) -0.4 - (6 + yToGo) * 0.025
    else 4
  }
}
