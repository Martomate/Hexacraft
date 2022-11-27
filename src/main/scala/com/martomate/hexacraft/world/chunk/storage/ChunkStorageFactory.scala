package com.martomate.hexacraft.world.chunk.storage

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.Blocks

import com.flowpowered.nbt.CompoundTag

trait ChunkStorageFactory {
  def empty(using CylinderSize, Blocks): ChunkStorage
  def fromNBT(nbt: CompoundTag)(using CylinderSize, Blocks): ChunkStorage
}
