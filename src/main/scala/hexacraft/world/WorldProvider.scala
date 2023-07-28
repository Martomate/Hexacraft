package hexacraft.world

import hexacraft.world.settings.WorldInfo

import com.flowpowered.nbt.CompoundTag

trait WorldProvider {
  def getWorldInfo: WorldInfo

  def loadState(path: String): CompoundTag
  def saveState(tag: CompoundTag, path: String): Unit
}
