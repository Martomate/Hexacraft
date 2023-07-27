package com.martomate.hexacraft.world.settings

import com.flowpowered.nbt.{CompoundTag, DoubleTag, LongTag}
import com.martomate.hexacraft.nbt.{Nbt, NBTUtil}

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
  def fromNBT(nbt2: CompoundTag, defaultSettings: WorldSettings): WorldGenSettings = {
    val nbt = Nbt.from(nbt2)
    new WorldGenSettings(
      nbt.getLong("seed", defaultSettings.seed.getOrElse(new Random().nextLong)),
      nbt.getDouble("blockGenScale", 0.1),
      nbt.getDouble("heightMapGenScale", 0.02),
      nbt.getDouble("blockDensityGenScale", 0.01),
      nbt.getDouble("biomeHeightMapGenScale", 0.002),
      nbt.getDouble("biomeHeightVariationGenScale", 0.002)
    )
  }
}
