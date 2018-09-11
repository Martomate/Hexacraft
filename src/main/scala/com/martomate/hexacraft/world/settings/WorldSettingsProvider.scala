package com.martomate.hexacraft.world.settings

import com.flowpowered.nbt.CompoundTag
import com.martomate.hexacraft.util.CylinderSize

trait WorldSettingsProvider {
  def name: String
  def size: CylinderSize
  def gen: WorldGenSettings
  def playerNBT: CompoundTag

  def loadState(path: String): CompoundTag
  def saveState(tag: CompoundTag, path: String): Unit
}
