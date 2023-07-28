package hexacraft.world.chunk

import hexacraft.nbt.Nbt
import hexacraft.world.{BlocksInWorld, CylinderSize, WorldProvider}
import hexacraft.world.block.{Blocks, BlockState}
import hexacraft.world.chunk.storage.{ChunkStorage, DenseChunkStorage}
import hexacraft.world.coord.integer.{BlockRelChunk, ChunkRelWorld}
import hexacraft.world.entity.EntityRegistry
import hexacraft.world.gen.WorldGenerator

import com.flowpowered.nbt.CompoundTag

class ChunkGenerator(
    coords: ChunkRelWorld,
    world: BlocksInWorld,
    worldProvider: WorldProvider,
    worldGenerator: WorldGenerator,
    registry: EntityRegistry
)(using cylSize: CylinderSize, Blocks: Blocks) {

  private def filePath: String = "data/" + coords.getColumnRelWorld.value + "/" + coords.Y.repr.toInt + ".dat"

  def loadData(): ChunkData = {
    val nbt = worldProvider.loadState(filePath)

    if (!nbt.getValue.isEmpty) {
      ChunkData.fromNBT(Nbt.from(nbt))(registry)
    } else {
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

      ChunkData.fromStorage(storage)
    }
  }

  def saveData(data: CompoundTag): Unit = {
    worldProvider.saveState(data, filePath)
  }

  private def getBlockAtDepth(yToGo: Int) = {
    if (yToGo < -5) Blocks.Stone
    else if (yToGo < -1) Blocks.Dirt
    else Blocks.Grass
  }

  private def limitForBlockNoise(yToGo: Int): Double = {
    if (yToGo < -6) -0.4
    else if (yToGo < 0) -0.4 - (6 + yToGo) * 0.025
    else 4
  }
}
