package com.martomate.hexacraft.world.chunk

import com.flowpowered.nbt.CompoundTag
import com.martomate.hexacraft.world.storage.ChunkData

trait IChunkGenerator {
  def loadData(): ChunkData
  def saveData(data: CompoundTag): Unit
}
