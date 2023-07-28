package hexacraft.world.gen

import com.flowpowered.nbt.CompoundTag
import hexacraft.world.CylinderSize
import hexacraft.world.coord.fp.BlockCoords
import hexacraft.world.coord.integer.{ChunkRelWorld, ColumnRelWorld}
import hexacraft.world.gen.noise.{NoiseGenerator3D, NoiseGenerator4D, NoiseInterpolator2D, NoiseInterpolator3D}
import hexacraft.world.settings.WorldGenSettings

import java.util.Random

class WorldGenerator(worldGenSettings: WorldGenSettings)(implicit worldSize: CylinderSize) {
  private val randomGenSeed = worldGenSettings.seed
  private val random = new Random(randomGenSeed)
  private val blockGenerator = new NoiseGenerator4D(random, 8, worldGenSettings.blockGenScale)
  private val heightMapGenerator =
    new NoiseGenerator3D(random, 8, worldGenSettings.heightMapGenScale)
  private val blockDensityGenerator =
    new NoiseGenerator4D(random, 4, worldGenSettings.blockDensityGenScale)
  private val biomeHeightGenerator =
    new NoiseGenerator3D(random, 4, worldGenSettings.biomeHeightMapGenScale)
  private val biomeHeightVariationGenerator =
    new NoiseGenerator3D(random, 4, worldGenSettings.biomeHeightVariationGenScale)

  def getHeightmapInterpolator(coords: ColumnRelWorld): NoiseInterpolator2D =
    new NoiseInterpolator2D(
      4,
      4,
      (i, j) => {
        val c = BlockCoords(coords.X * 16 + i * 4, 0, coords.Z * 16 + j * 4).toCylCoords
        val height = biomeHeightGenerator.genNoiseFromCylXZ(c)
        val heightVariation = biomeHeightVariationGenerator.genNoiseFromCylXZ(c)
        heightMapGenerator.genNoiseFromCylXZ(c) * heightVariation * 100 + height * 100
      }
    )

  def getBlockInterpolator(coords: ChunkRelWorld): NoiseInterpolator3D = new NoiseInterpolator3D(
    4,
    4,
    4,
    (i, j, k) => {
      val c =
        BlockCoords(coords.X * 16 + i * 4, coords.Y * 16 + j * 4, coords.Z * 16 + k * 4).toCylCoords
      blockGenerator.genNoiseFromCyl(c) + blockDensityGenerator.genNoiseFromCyl(c) * 0.4
    }
  )

  def toNBT: CompoundTag = worldGenSettings.toNBT
}
