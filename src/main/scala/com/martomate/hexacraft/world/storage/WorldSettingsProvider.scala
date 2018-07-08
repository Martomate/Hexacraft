package com.martomate.hexacraft.world.storage

import java.io.File
import java.util.Random

import com.flowpowered.nbt.{CompoundTag, DoubleTag, LongTag}
import com.martomate.hexacraft.util.NBTUtil
import com.martomate.hexacraft.world.WorldSettings
import com.martomate.hexacraft.worldsave.WorldSave

class WorldGenSettings(val nbt: CompoundTag, val defaultSettings: WorldSettings) {
  val seed                        : Long   = NBTUtil.getLong  (nbt, "seed", defaultSettings.seed.getOrElse(new Random().nextLong))
  val blockGenScale               : Double = NBTUtil.getDouble(nbt, "blockGenScale", 0.1)
  val heightMapGenScale           : Double = NBTUtil.getDouble(nbt, "heightMapGenScale", 0.02)
  val blockDensityGenScale        : Double = NBTUtil.getDouble(nbt, "blockDensityGenScale", 0.01)
  val biomeHeightMapGenScale      : Double = NBTUtil.getDouble(nbt, "biomeHeightMapGenScale", 0.002)
  val biomeHeightVariationGenScale: Double = NBTUtil.getDouble(nbt, "biomeHeightVariationGenScale", 0.002)

  def toNBT: CompoundTag = NBTUtil.makeCompoundTag("gen", Seq(
    new LongTag("seed", seed),
    new DoubleTag("blockGenScale", blockGenScale),
    new DoubleTag("heightMapGenScale", heightMapGenScale),
    new DoubleTag("blockDensityGenScale", blockDensityGenScale),
    new DoubleTag("biomeHeightGenScale", biomeHeightMapGenScale),
    new DoubleTag("biomeHeightVariationGenScale", biomeHeightVariationGenScale)
  ))
}

trait WorldSettingsProvider {
  def name: String
  def size: CylinderSize
  def gen: WorldGenSettings
  def playerNBT: CompoundTag

  def loadState(path: String): CompoundTag
  def saveState(tag: CompoundTag, path: String = "world.dat"): Unit
}

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

  def saveState(tag: CompoundTag, path: String = "world.dat"): Unit = {
    NBTUtil.saveTag(tag, new File(saveDir, path))
  }
}
