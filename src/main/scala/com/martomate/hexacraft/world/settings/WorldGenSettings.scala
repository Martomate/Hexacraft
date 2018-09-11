package com.martomate.hexacraft.world.settings

import java.util.Random

import com.flowpowered.nbt.{CompoundTag, DoubleTag, LongTag}
import com.martomate.hexacraft.util.NBTUtil

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
