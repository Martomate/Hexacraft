package com.martomate.hexacraft.world

import com.flowpowered.nbt.CompoundTag
import com.martomate.hexacraft.util.{CylinderSize, NBTUtil}
import com.martomate.hexacraft.world.settings.{WorldGenSettings, WorldInfo, WorldProvider}

class FakeWorldProvider(seed: Long)(implicit cylSize: CylinderSize) extends WorldProvider {
  override def getWorldInfo: WorldInfo = new WorldInfo(
    "test world",
    cylSize,
    new WorldGenSettings(seed, 0.1, 0.01, 0.01, 0.001, 0.001),
    NBTUtil.makeCompoundTag("", Seq.empty),
    NBTUtil.makeCompoundTag("", Seq.empty))

  private var fs: Map[String, CompoundTag] = Map.empty

  override def loadState(path: String): CompoundTag = fs.getOrElse(path, NBTUtil.makeCompoundTag("", Seq.empty))

  override def saveState(tag: CompoundTag, path: String): Unit = fs += path -> tag
}