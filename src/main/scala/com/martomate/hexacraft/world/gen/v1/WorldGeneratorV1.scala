package com.martomate.hexacraft.world.gen.v1

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.{Blocks, BlockState}
import com.martomate.hexacraft.world.chunk.storage.{ChunkStorage, DenseChunkStorage}
import com.martomate.hexacraft.world.coord.fp.BlockCoords
import com.martomate.hexacraft.world.coord.integer.{BlockRelChunk, ChunkRelWorld, ColumnRelWorld}
import com.martomate.hexacraft.world.gen.WorldGeneratorStrategy
import com.martomate.hexacraft.world.gen.noise.*
import com.martomate.hexacraft.world.settings.WorldGenV1

import java.util.Random

class WorldGeneratorV1(seed: Long, settings: WorldGenV1)(using CylinderSize)(using Blocks: Blocks)
    extends WorldGeneratorStrategy {
  private val randomGenSeed = seed
  private val random = new Random(randomGenSeed)
  private val blockGenerator = new NoiseGenerator4D(random, 8, settings.blockGenScale)
  private val heightMapGenerator =
    new NoiseGenerator3D(random, 8, settings.heightMapGenScale)
  private val blockDensityGenerator =
    new NoiseGenerator4D(random, 4, settings.blockDensityGenScale)
  private val biomeHeightGenerator =
    new NoiseGenerator3D(random, 4, settings.biomeHeightMapGenScale)
  private val biomeHeightVariationGenerator =
    new NoiseGenerator3D(random, 4, settings.biomeHeightVariationGenScale)

  def heightMap(coords: ColumnRelWorld): HeightMap =
    val interp = getHeightmapInterpolator(coords)

    for (x <- 0 until 16) yield {
      for (z <- 0 until 16) yield {
        interp(x, z).toShort
      }
    }

  private def getHeightmapInterpolator(coords: ColumnRelWorld): NoiseInterpolator2D =
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

  def blocksInChunk(coords: ChunkRelWorld, heightMap: HeightMap): ChunkStorage =
    val blockNoise = getBlockInterpolator(coords)

    val storage: ChunkStorage = new DenseChunkStorage
    for (i <- 0 until 16; j <- 0 until 16; k <- 0 until 16) {
      val noise = blockNoise(i, j, k)
      val yToGo = coords.Y * 16 + j - heightMap(i)(k)
      val limit = limitForBlockNoise(yToGo)
      if (noise > limit)
        storage.setBlock(BlockRelChunk(i, j, k), new BlockState(getBlockAtDepth(yToGo)))
    }
    storage

  private def getBlockAtDepth(yToGo: Int) =
    if (yToGo < -5) Blocks.Stone
    else if (yToGo < -1) Blocks.Dirt
    else Blocks.Grass

  private def limitForBlockNoise(yToGo: Int): Double =
    if (yToGo < -6) -0.4
    else if (yToGo < 0) -0.4 - (6 + yToGo) * 0.025
    else 4

  private def getBlockInterpolator(coords: ChunkRelWorld): NoiseInterpolator3D = new NoiseInterpolator3D(
    4,
    4,
    4,
    (i, j, k) => {
      val c =
        BlockCoords(coords.X * 16 + i * 4, coords.Y * 16 + j * 4, coords.Z * 16 + k * 4).toCylCoords
      blockGenerator.genNoiseFromCyl(c) + blockDensityGenerator.genNoiseFromCyl(c) * 0.4
    }
  )
}
