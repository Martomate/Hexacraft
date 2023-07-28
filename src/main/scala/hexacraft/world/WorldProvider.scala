package hexacraft.world

import com.flowpowered.nbt.CompoundTag
import hexacraft.world.settings.WorldInfo

trait WorldProvider {
  def getWorldInfo: WorldInfo

  def loadState(path: String): CompoundTag
  def saveState(tag: CompoundTag, path: String): Unit
}
