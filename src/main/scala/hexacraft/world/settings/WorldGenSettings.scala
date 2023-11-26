package hexacraft.world.settings

import hexacraft.nbt.Nbt

import java.util.Random

class WorldGenSettings(
    val seed: Long,
    val blockGenScale: Double,
    val heightMapGenScale: Double,
    val blockDensityGenScale: Double,
    val biomeHeightMapGenScale: Double,
    val biomeHeightVariationGenScale: Double
) {

  def toNBT: Nbt.MapTag = Nbt.makeMap(
    "seed" -> Nbt.LongTag(seed),
    "blockGenScale" -> Nbt.DoubleTag(blockGenScale),
    "heightMapGenScale" -> Nbt.DoubleTag(heightMapGenScale),
    "blockDensityGenScale" -> Nbt.DoubleTag(blockDensityGenScale),
    "biomeHeightGenScale" -> Nbt.DoubleTag(biomeHeightMapGenScale),
    "biomeHeightVariationGenScale" -> Nbt.DoubleTag(biomeHeightVariationGenScale)
  )
}

object WorldGenSettings {
  def fromNBT(nbt: Nbt.MapTag, defaultSettings: WorldSettings): WorldGenSettings = {
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
