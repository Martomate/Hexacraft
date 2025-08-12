package hexacraft.world

import hexacraft.math.{Range2D, Range3D}
import hexacraft.math.noise.{Data2D, Data3D, NoiseGenerator3D, NoiseGenerator4D}
import hexacraft.util.Loop
import hexacraft.world.WorldGenerator.Pos
import hexacraft.world.block.{Block, BlockState}
import hexacraft.world.chunk.{Biome, ChunkColumnTerrain, ChunkStorage, DenseChunkStorage}
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

  private val humidityGenerator =
    new NoiseGenerator3D(random, 2, worldGenSettings.humidityGenScale)

  private val temperatureGenerator =
    new NoiseGenerator3D(random, 2, worldGenSettings.temperatureGenScale)

  def getHumidityForColumn(coords: ColumnRelWorld): Data2D = {
    WorldGenerator.makeSampler2D(coords, Pos.eval2(_.evalXZ(humidityGenerator)))
  }

  def getTemperatureForColumn(coords: ColumnRelWorld): Data2D = {
    WorldGenerator.makeSampler2D(coords, Pos.eval2(_.evalXZ(temperatureGenerator)))
  }

  def getHeightmapInterpolator(coords: ColumnRelWorld): Data2D = {
    WorldGenerator.makeSampler2D(coords, terrainHeight)
  }

  private def blockNoise(x: Int, y: Int, z: Int) =
    Pos.eval(BlockCoords(x, y, z)) { pos =>
      val n1 = pos.evalXYZ(blockGenerator)
      val n2 = pos.evalXYZ(blockDensityGenerator)

      n1 + n2 * 0.4
    }

  private def terrainHeight(x: Int, z: Int) =
    Pos.eval(BlockCoords(x, 0, z)) { pos =>
      val biomeHeight = pos.evalXZ(biomeHeightGenerator)
      val heightVariation = pos.evalXZ(biomeHeightVariationGenerator)
      val heightMap = pos.evalXZ(heightMapGenerator)

      heightMap * heightVariation * 100 + biomeHeight * 100
    }

  def generateChunk(coords: ChunkRelWorld, column: ChunkColumnTerrain): ChunkStorage = {
    val storage: ChunkStorage = new DenseChunkStorage
    val blockNoise = WorldGenerator.makeSampler3D(coords, this.blockNoise)

    Loop.rangeUntil(0, 16) { i =>
      Loop.rangeUntil(0, 16) { k =>
        val groundLevel = column.originalTerrainHeight.getHeight(i, k)

        Loop.rangeUntil(0, 16) { j =>
          val noise = blockNoise(i, j, k)
          val yToGo = coords.Y.toInt * 16 + j - groundLevel
          val limit = limitForBlockNoise(yToGo)
          val biome = column.biome(i, k)
          if noise > limit then {
            storage.setBlock(BlockRelChunk(i, j, k), new BlockState(getBlockAtDepth(yToGo, biome)))
          } else if biome == Biome.Ocean && groundLevel + yToGo <= 0 then {
            storage.setBlock(BlockRelChunk(i, j, k), new BlockState(Block.Water))
          }
        }
      }
    }

    storage
  }

  private def getBlockAtDepth(yToGo: Int, biome: Biome) = {
    biome match {
      case Biome.Ocean =>
        if yToGo < -2 then {
          Block.Stone
        } else {
          Block.Sand
        }
      case Biome.Desert =>
        if yToGo < -5 then {
          Block.Stone
        } else {
          Block.Sand
        }
      case _ =>
        if yToGo < -5 then {
          Block.Stone
        } else if yToGo < -1 then {
          Block.Dirt
        } else {
          Block.Grass
        }
    }
  }

  private def limitForBlockNoise(yToGo: Int): Double = {
    if yToGo < -6 then {
      -0.4
    } else if yToGo < 0 then {
      -0.4 - (6 + yToGo) * 0.25
    } else {
      4
    }
  }
}

object WorldGenerator {
  private val samplingPoints2D = Range2D(
    0 to 16 by 4,
    0 to 16 by 4
  )

  inline def makeSampler2D(coords: ColumnRelWorld, inline noise: (Int, Int) => Double): Data2D = {
    val sx = coords.X.toInt * 16
    val sz = coords.Z.toInt * 16

    val data = Data2D.evaluate(samplingPoints2D, (cx, cz) => noise(sx + cx, sz + cz))
    Data2D.interpolate(4, 4, data)
  }

  private val samplingPoints3D = Range3D(
    0 to 16 by 4,
    0 to 16 by 4,
    0 to 16 by 4
  )

  inline def makeSampler3D(coords: ChunkRelWorld, inline noise: (Int, Int, Int) => Double): Data3D = {
    val sx = coords.X.toInt * 16
    val sy = coords.Y.toInt * 16
    val sz = coords.Z.toInt * 16

    val data = Data3D.evaluate(samplingPoints3D, (cx, cy, cz) => noise(sx + cx, sy + cy, sz + cz))
    Data3D.interpolate(4, 4, 4, data)
  }

  class Pos(val x: Double, val y: Double, val z: Double, radius: Double) {
    def evalXZ(noise: NoiseGenerator3D): Double = noise.genWrappedNoise(x, z, radius)
    def evalXYZ(noise: NoiseGenerator4D): Double = noise.genWrappedNoise(x, y, z, radius)
  }

  object Pos {
    inline def eval(bc: BlockCoords)(inline f: Pos => Double)(using cylSize: CylinderSize): Double = {
      val c = bc.toCylCoords
      f(Pos(c.x, c.y, c.z, cylSize.radius))
    }

    inline def eval2(inline f: Pos => Double)(using cylSize: CylinderSize): (Int, Int) => Double = { (x, z) =>
      val bc = BlockCoords(x, 0, z)
      val c = bc.toCylCoords
      f(Pos(c.x, c.y, c.z, cylSize.radius))
    }
  }
}
