package com.martomate.hexacraft.world.settings

import com.martomate.hexacraft.util.NBTUtil

import com.flowpowered.nbt.{CompoundTag, DoubleTag, LongTag}
import java.util.Random

class WorldGenSettings(
    val seed: Long,
    val blockGenScale: Double,
    val heightMapGenScale: Double,
    val blockDensityGenScale: Double,
    val biomeHeightMapGenScale: Double,
    val biomeHeightVariationGenScale: Double
) {

  def toNBT: CompoundTag = NBTUtil.makeCompoundTag(
    "gen",
    Seq(
      new LongTag("seed", seed),
      new DoubleTag("blockGenScale", blockGenScale),
      new DoubleTag("heightMapGenScale", heightMapGenScale),
      new DoubleTag("blockDensityGenScale", blockDensityGenScale),
      new DoubleTag("biomeHeightGenScale", biomeHeightMapGenScale),
      new DoubleTag("biomeHeightVariationGenScale", biomeHeightVariationGenScale)
    )
  )
}

object WorldGenSettings {
  def fromNBT(nbt: CompoundTag, defaultSettings: WorldSettings): WorldGenSettings = {
    new WorldGenSettings(
      NBTUtil.getLong(nbt, "seed", defaultSettings.seed.getOrElse(new Random().nextLong)),
      NBTUtil.getDouble(nbt, "blockGenScale", 0.1),
      NBTUtil.getDouble(nbt, "heightMapGenScale", 0.02),
      NBTUtil.getDouble(nbt, "blockDensityGenScale", 0.01),
      NBTUtil.getDouble(nbt, "biomeHeightMapGenScale", 0.002),
      NBTUtil.getDouble(nbt, "biomeHeightVariationGenScale", 0.002)
    )
  }
}
