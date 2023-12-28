package hexacraft.world

import hexacraft.math.{Range2D, Range3D}
import hexacraft.math.noise.{Data2D, Data3D, NoiseGenerator3D, NoiseGenerator4D}
import hexacraft.world.block.{Block, BlockState}
import hexacraft.world.chunk.{ChunkColumnTerrain, ChunkData, ChunkStorage, DenseChunkStorage}
import hexacraft.world.coord.{BlockCoords, BlockRelChunk, ChunkRelWorld, ColumnRelWorld}

import java.util.Random

class WorldGenerator(worldGenSettings: WorldGenSettings)(using CylinderSize) {
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

  def getHeightmapInterpolator(coords: ColumnRelWorld): Data2D =
    WorldGenerator.makeHeightmapInterpolator(coords, terrainHeight)

  def getBlockInterpolator(coords: ChunkRelWorld): Data3D =
    WorldGenerator.makeBlockInterpolator(coords, blockNoise)

  private def blockNoise(x: Int, y: Int, z: Int) =
    val c = BlockCoords(x, y, z).toCylCoords
    blockGenerator.genNoiseFromCyl(c) + blockDensityGenerator.genNoiseFromCyl(c) * 0.4

  private def terrainHeight(x: Int, z: Int) =
    val c = BlockCoords(x, 0, z).toCylCoords
    val height = biomeHeightGenerator.genNoiseFromCylXZ(c)
    val heightVariation = biomeHeightVariationGenerator.genNoiseFromCylXZ(c)
    heightMapGenerator.genNoiseFromCylXZ(c) * heightVariation * 100 + height * 100

  def generateChunk(coords: ChunkRelWorld, column: ChunkColumnTerrain): ChunkData =
    val storage: ChunkStorage = new DenseChunkStorage
    val blockNoise = this.getBlockInterpolator(coords)

    for (i <- 0 until 16; j <- 0 until 16; k <- 0 until 16) {
      val noise = blockNoise(i, j, k)
      val yToGo = coords.Y.toInt * 16 + j - column.originalTerrainHeight(i, k)
      val limit = limitForBlockNoise(yToGo)
      if (noise > limit)
        storage.setBlock(BlockRelChunk(i, j, k), new BlockState(getBlockAtDepth(yToGo)))
    }

    val data = ChunkData.fromStorage(storage)
    data

  private def getBlockAtDepth(yToGo: Int) = {
    if (yToGo < -5) Block.Stone
    else if (yToGo < -1) Block.Dirt
    else Block.Grass
  }

  private def limitForBlockNoise(yToGo: Int): Double = {
    if (yToGo < -6) -0.4
    else if (yToGo < 0) -0.4 - (6 + yToGo) * 0.025
    else 4
  }
}

object WorldGenerator {
  def makeHeightmapInterpolator(coords: ColumnRelWorld, terrainHeight: (Int, Int) => Double): Data2D =
    val samplingPoints = Range2D(
      0 to 16 by 4,
      0 to 16 by 4
    ).offset(
      coords.X.toInt * 16,
      coords.Z.toInt * 16
    )

    val samples = Data2D.evaluate(samplingPoints, terrainHeight)
    Data2D.interpolate(4, 4, samples)

  def makeBlockInterpolator(coords: ChunkRelWorld, noise: (Int, Int, Int) => Double): Data3D =
    val samplingPoints = Range3D(
      0 to 16 by 4,
      0 to 16 by 4,
      0 to 16 by 4
    ).offset(
      coords.X.toInt * 16,
      coords.Y.toInt * 16,
      coords.Z.toInt * 16
    )

    val samples = Data3D.evaluate(samplingPoints, noise)
    Data3D.interpolate(4, 4, 4, samples)
}
