package com.martomate.hexacraft.world.chunk

import com.flowpowered.nbt.CompoundTag
import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.{BlocksInWorld, WorldProvider}
import com.martomate.hexacraft.world.block.{BlockState, Blocks}
import com.martomate.hexacraft.world.chunk.storage.{ChunkStorage, DenseChunkStorage}
import com.martomate.hexacraft.world.coord.integer.{BlockRelChunk, ChunkRelWorld}
import com.martomate.hexacraft.world.entity.{EntityModelLoader, EntityRegistry}
import com.martomate.hexacraft.world.gen.WorldGenerator

class ChunkGenerator(
    coords: ChunkRelWorld,
    world: BlocksInWorld,
    worldProvider: WorldProvider,
    worldGenerator: WorldGenerator,
    registry: EntityRegistry
)(using EntityModelLoader)(using cylSize: CylinderSize, Blocks: Blocks) {

  private def filePath: String =
    "data/" + coords.getColumnRelWorld.value + "/" + coords.getChunkRelColumn.value + ".dat"

  def loadData(): ChunkData = {
    val nbt = worldProvider.loadState(filePath)

    if (!nbt.getValue.isEmpty) {
      ChunkData.fromNBT(nbt)(coords, registry)
    } else {
      val storage: ChunkStorage = new DenseChunkStorage(coords)
      val column = world.provideColumn(coords.getColumnRelWorld)
      val blockNoise = worldGenerator.getBlockInterpolator(coords)

      for (i <- 0 until 16; j <- 0 until 16; k <- 0 until 16) {
        val noise = blockNoise(i, j, k)
        val yToGo = coords.Y * 16 + j - column.generatedHeightMap(i)(k)
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
