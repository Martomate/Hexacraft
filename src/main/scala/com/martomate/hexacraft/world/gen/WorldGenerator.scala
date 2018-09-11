package com.martomate.hexacraft.world.gen

import java.util.Random

import com.flowpowered.nbt.CompoundTag
import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.coord.fp.BlockCoords
import com.martomate.hexacraft.world.coord.integer.{ChunkRelWorld, ColumnRelWorld}
import com.martomate.hexacraft.world.gen.noise.{NoiseGenerator3D, NoiseGenerator4D, NoiseInterpolator2D, NoiseInterpolator3D}
import com.martomate.hexacraft.world.settings.WorldGenSettings

class WorldGenerator(worldGenSettings: WorldGenSettings, worldSize: CylinderSize) {
  private val randomGenSeed = worldGenSettings.seed
  private val random = new Random(randomGenSeed)
  private val blockGenerator                = new NoiseGenerator4D(random, 8, worldGenSettings.blockGenScale)
  private val heightMapGenerator            = new NoiseGenerator3D(random, 8, worldGenSettings.heightMapGenScale)
  private val blockDensityGenerator         = new NoiseGenerator4D(random, 4, worldGenSettings.blockDensityGenScale)
  private val biomeHeightGenerator          = new NoiseGenerator3D(random, 4, worldGenSettings.biomeHeightMapGenScale)
  private val biomeHeightVariationGenerator = new NoiseGenerator3D(random, 4, worldGenSettings.biomeHeightVariationGenScale)

  def getHeightmapInterpolator(coords: ColumnRelWorld): NoiseInterpolator2D = new NoiseInterpolator2D(4, 4, (i, j) => {
    val c = BlockCoords(coords.X * 16 + i * 4, 0, coords.Z * 16 + j * 4, worldSize).toCylCoords
    val height = biomeHeightGenerator.genNoiseFromCylXZ(c)
    val heightVariation = biomeHeightVariationGenerator.genNoiseFromCylXZ(c)
    heightMapGenerator.genNoiseFromCylXZ(c) * heightVariation * 100 + height * 100
  })

  def getBlockInterpolator(coords: ChunkRelWorld): NoiseInterpolator3D = new NoiseInterpolator3D(4, 4, 4, (i, j, k) => {
    val c = BlockCoords(coords.X * 16 + i * 4, coords.Y * 16 + j * 4, coords.Z * 16 + k * 4, worldSize).toCylCoords
    blockGenerator.genNoiseFromCyl(c) + blockDensityGenerator.genNoiseFromCyl(c) * 0.4
  })

  def toNBT: CompoundTag = worldGenSettings.toNBT
}
