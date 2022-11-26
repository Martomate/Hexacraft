package com.martomate.hexacraft.world.chunk.storage

import com.flowpowered.nbt.CompoundTag
import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.Blocks

trait ChunkStorageFactory {
  def empty(using CylinderSize, Blocks): ChunkStorage
  def fromNBT(nbt: CompoundTag)(using CylinderSize, Blocks): ChunkStorage
}
