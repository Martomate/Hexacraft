package com.martomate.hexacraft.world.settings

import java.io.File

import com.flowpowered.nbt.CompoundTag
import com.martomate.hexacraft.util.{CylinderSize, NBTUtil}
import com.martomate.hexacraft.world.save.WorldSave

class WorldSettingsProviderFromFile(saveDir: File, worldSettings: WorldSettings) extends WorldSettingsProvider {
  WorldSave(saveDir)// TODO: Use this class instead of all the current stuff
  private val nbtData: CompoundTag = loadState("world.dat")
  private val generalSettings: CompoundTag = nbtData.getValue.get("general").asInstanceOf[CompoundTag]

  val name: String = NBTUtil.getString(generalSettings, "worldName", worldSettings.name.getOrElse(saveDir.getName))
  val size: CylinderSize = new CylinderSize(NBTUtil.getByte(generalSettings, "worldSize", worldSettings.size.getOrElse(7)))
  def gen: WorldGenSettings = new WorldGenSettings(nbtData.getValue.get("gen").asInstanceOf[CompoundTag], worldSettings)
  def playerNBT: CompoundTag = nbtData.getValue.get("player").asInstanceOf[CompoundTag]

  def loadState(path: String): CompoundTag = {
    NBTUtil.loadTag(new File(saveDir, path))
  }

  def saveState(tag: CompoundTag, path: String): Unit = {
    NBTUtil.saveTag(tag, new File(saveDir, path))
  }
}
