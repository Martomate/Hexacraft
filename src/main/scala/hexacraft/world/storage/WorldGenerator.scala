package hexacraft.world.storage

import java.util.Random

import com.flowpowered.nbt.{CompoundTag, DoubleTag, LongTag}
import hexacraft.util.NBTUtil
import hexacraft.world.WorldSettings
import hexacraft.world.gen.noise.{NoiseGenerator3D, NoiseGenerator4D}

class WorldGenerator(worldGenSettings: CompoundTag, worldSettings: WorldSettings) {
  private val randomGenSeed = NBTUtil.getLong(worldGenSettings, "seed", worldSettings.seed.getOrElse(new Random().nextLong))
  private val random = new Random(randomGenSeed)
  private[storage] val blockGenerator                = new NoiseGenerator4D(random, 8, NBTUtil.getDouble(worldGenSettings, "blockGenScale", 0.1))
  private[storage] val heightMapGenerator            = new NoiseGenerator3D(random, 8, NBTUtil.getDouble(worldGenSettings, "heightMapGenScale", 0.02))
  private[storage] val blockDensityGenerator         = new NoiseGenerator4D(random, 4, NBTUtil.getDouble(worldGenSettings, "blockDensityGenScale", 0.01))
  private[storage] val biomeHeightGenerator          = new NoiseGenerator3D(random, 4, NBTUtil.getDouble(worldGenSettings, "biomeHeightMapGenScale", 0.002))
  private[storage] val biomeHeightVariationGenerator = new NoiseGenerator3D(random, 4, NBTUtil.getDouble(worldGenSettings, "biomeHeightVariationGenScale", 0.002))

  def saveInTag(): CompoundTag = {
    NBTUtil.makeCompoundTag("gen", Seq(
      new LongTag("seed", randomGenSeed),
      new DoubleTag("blockGenScale", blockGenerator.scale),
      new DoubleTag("heightMapGenScale", heightMapGenerator.scale),
      new DoubleTag("blockDensityGenScale", blockDensityGenerator.scale),
      new DoubleTag("biomeHeightGenScale", biomeHeightGenerator.scale),
      new DoubleTag("biomeHeightVariationGenScale", biomeHeightVariationGenerator.scale)
    ))
  }
}
