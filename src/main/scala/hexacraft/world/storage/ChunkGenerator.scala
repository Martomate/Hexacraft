package hexacraft.world.storage

import com.flowpowered.nbt.CompoundTag
import hexacraft.block.{Block, BlockState}
import hexacraft.world.coord.BlockRelChunk

class ChunkGenerator(chunk: Chunk) {
  def loadData(): ChunkData = {
    val world = chunk.world
    val coords = chunk.coords
    val nbt = world.worldSettings.loadState("chunks/" + coords.value + ".dat")

    val storage: ChunkStorage = new DenseChunkStorage(chunk.coords)
    if (!nbt.getValue.isEmpty) {
      storage.fromNBT(nbt)
    } else {
      val column = world.getColumn(coords.getColumnRelWorld).get

      val blockNoise = world.worldGenerator.getBlockInterpolator(coords)

      for (i <- 0 until 16; j <- 0 until 16; k <- 0 until 16) {
        val noise = blockNoise(i, j, k)
        val yToGo = coords.Y * 16 + j - column.heightMap(i, k)
        val limit = limitForBlockNoise(yToGo)
        if (noise > limit) storage.setBlock(BlockRelChunk(i, j, k, world.size), new BlockState(getBlockAtDepth(yToGo)))
      }
    }
    val data = new ChunkData
    data.storage = storage
    data
  }

  def saveData(data: CompoundTag): Unit = {
    chunk.world.worldSettings.saveState(data, "chunks/" + chunk.coords.value + ".dat")
  }

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

class ChunkData {
  var storage: ChunkStorage = _

  def optimizeStorage(): Unit = {
    if (storage.isDense) {
      if (storage.numBlocks < 48) {
        storage = storage.toSparse
      }
    } else {
      if (storage.numBlocks > 64) {
        storage = storage.toDense
      }
    }
  }
}