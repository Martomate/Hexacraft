package hexacraft.world

import hexacraft.math.{Range2D, Range3D}
import hexacraft.math.noise.{Data2D, Data3D, NoiseGenerator3D, NoiseGenerator4D}
import hexacraft.util.Loop
import hexacraft.world.block.{Block, BlockState}
import hexacraft.world.chunk.{ChunkColumnTerrain, ChunkStorage, DenseChunkStorage}
import hexacraft.world.coord.{BlockCoords, BlockRelChunk, ChunkRelWorld, ColumnRelWorld}

import java.util.Random

class WorldGenerator(worldGenSettings: WorldGenSettings)(using cylSize: CylinderSize) {
  private val random = new Random(worldGenSettings.seed)

  private val blockGenerator =
    new NoiseGenerator4D(random, 8, worldGenSettings.blockGenScale)

  private val heightMapGenerator =
    new NoiseGenerator3D(random, 8, worldGenSettings.heightMapGenScale)

  private val blockDensityGenerator =
    new NoiseGenerator4D(random, 4, worldGenSettings.blockDensityGenScale)

  private val biomeHeightGenerator =
    new NoiseGenerator3D(random, 4, worldGenSettings.biomeHeightMapGenScale)

  private val biomeHeightVariationGenerator =
    new NoiseGenerator3D(random, 4, worldGenSettings.biomeHeightVariationGenScale)

  def getHeightmapInterpolator(coords: ColumnRelWorld): Data2D = {
    WorldGenerator.makeHeightmapInterpolator(coords, terrainHeight)
  }

  private def blockNoise(x: Int, y: Int, z: Int) = {
    val c = BlockCoords(x, y, z).toCylCoords
    val radius = cylSize.radius
    val n1 = blockGenerator.genWrappedNoise(c.x, c.y, c.z, radius)
    val n2 = blockDensityGenerator.genWrappedNoise(c.x, c.y, c.z, radius)
    n1 + n2 * 0.4
  }

  private def terrainHeight(x: Int, z: Int) = {
    val c = BlockCoords(x, 0, z).toCylCoords
    val radius = cylSize.radius
    val biomeHeight = biomeHeightGenerator.genWrappedNoise(c.x, c.z, radius)
    val heightVariation = biomeHeightVariationGenerator.genWrappedNoise(c.x, c.z, radius)
    val heightMap = heightMapGenerator.genWrappedNoise(c.x, c.z, radius)
    heightMap * heightVariation * 100 + biomeHeight * 100
  }

  def generateChunk(coords: ChunkRelWorld, column: ChunkColumnTerrain): ChunkStorage = {
    val storage: ChunkStorage = new DenseChunkStorage
    val blockNoise = WorldGenerator.makeBlockInterpolator(coords, this.blockNoise)

    Loop.rangeUntil(0, 16) { i =>
      Loop.rangeUntil(0, 16) { j =>
        Loop.rangeUntil(0, 16) { k =>
          val noise = blockNoise(i, j, k)
          val yToGo = coords.Y.toInt * 16 + j - column.originalTerrainHeight.getHeight(i, k)
          val limit = limitForBlockNoise(yToGo)
          if noise > limit then {
            storage.setBlock(BlockRelChunk(i, j, k), new BlockState(getBlockAtDepth(yToGo)))
          }
        }
      }
    }

    storage
  }

  private def getBlockAtDepth(yToGo: Int) = {
    if yToGo < -5 then {
      Block.Stone
    } else if yToGo < -1 then {
      Block.Dirt
    } else {
      Block.Grass
    }
  }

  private def limitForBlockNoise(yToGo: Int): Double = {
    if yToGo < -6 then {
      -0.4
    } else if yToGo < 0 then {
      -0.4 - (6 + yToGo) * 0.025
    } else {
      4
    }
  }
}

object WorldGenerator {
  def makeHeightmapInterpolator(coords: ColumnRelWorld, terrainHeight: (Int, Int) => Double): Data2D = {
    val samplingPoints = Range2D(
      0 to 16 by 4,
      0 to 16 by 4
    ).offset(
      coords.X.toInt * 16,
      coords.Z.toInt * 16
    )

    val samples = Data2D.evaluate(samplingPoints, terrainHeight)
    Data2D.interpolate(4, 4, samples)
  }

  def makeBlockInterpolator(coords: ChunkRelWorld, noise: (Int, Int, Int) => Double): Data3D = {
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
}
