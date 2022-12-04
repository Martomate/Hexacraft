package com.martomate.hexacraft.world.chunk

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.{BlocksInWorld, WorldProvider}
import com.martomate.hexacraft.world.block.{Blocks, BlockState}
import com.martomate.hexacraft.world.chunk.storage.{ChunkStorage, DenseChunkStorage}
import com.martomate.hexacraft.world.coord.integer.{BlockRelChunk, ChunkRelWorld}
import com.martomate.hexacraft.world.entity.EntityRegistry
import com.martomate.hexacraft.world.gen.WorldGenerator

import com.flowpowered.nbt.CompoundTag

class ChunkGenerator(
    coords: ChunkRelWorld,
    world: BlocksInWorld,
    worldProvider: WorldProvider,
    worldGenerator: WorldGenerator,
    registry: EntityRegistry
)(using cylSize: CylinderSize, Blocks: Blocks) {

  private def filePath: String =
    "data/" + coords.getColumnRelWorld.value + "/" + coords.getChunkRelColumn.value + ".dat"

  def loadData(): ChunkData = {
    val nbt = worldProvider.loadState(filePath)

    if (!nbt.getValue.isEmpty) {
      ChunkData.fromNBT(nbt)(registry)
    } else {
      val column = world.provideColumn(coords.getColumnRelWorld)
      val storage: ChunkStorage = worldGenerator.blocksInChunk(coords, column.generatedHeightMap)
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
