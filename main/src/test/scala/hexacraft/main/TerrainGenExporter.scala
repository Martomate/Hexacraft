package hexacraft.main

import hexacraft.client.{BlockSpecs, BlockTextureLoader}
import hexacraft.math.Range2D
import hexacraft.math.noise.Data2D
import hexacraft.nbt.Nbt
import hexacraft.renderer.PixelArray
import hexacraft.server.world.plan.WorldPlanner
import hexacraft.util.Loop
import hexacraft.world.*
import hexacraft.world.block.BlockState
import hexacraft.world.chunk.{Biome, Chunk, ChunkColumnTerrain}
import hexacraft.world.coord.{BlockRelChunk, BlockRelWorld, ChunkRelWorld, ColumnRelWorld}

import org.joml.Vector3f

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import scala.collection.mutable

object TerrainGenExporter {
  given cylSize: CylinderSize = CylinderSize(6)

  private val seed = Some(-8783609377086790360L)

  private val blockLoader = BlockTextureLoader.instance
  private val blockSpecs = BlockSpecs.default
  private val blockTextureMapping = BlockTextureLoader.loadBlockTextures(blockSpecs, blockLoader).unwrap()
  private val blockTextureIndices: Map[String, IndexedSeq[Int]] =
    blockSpecs.view.mapValues(spec => spec.textures.indices(blockTextureMapping.texIdxMap)).toMap
  private val blockTextureColors: Map[String, IndexedSeq[Vector3f]] =
    blockTextureIndices.view
      .mapValues(indices => indices.map(idx => calculateTextureColor(blockTextureMapping.images(idx & 0xfff))))
      .toMap

  private val genSettings = WorldGenSettings.fromNBT(Nbt.emptyMap, WorldSettings.none.copy(seed = seed))
  private val gen = WorldGenerator(genSettings)

  def main(args: Array[String]): Unit = {
    val xChunks = 512
    val zChunks = cylSize.ringSize.min(64)

    exportHeightMap(xChunks, zChunks)
    exportBiomeMap(xChunks, zChunks)
    exportBlockMap(xChunks, zChunks)
  }

  private def exportHeightMap(xChunks: Int, zChunks: Int): Unit = {
    val image = BufferedImage(16 * xChunks, 16 * zChunks, BufferedImage.TYPE_INT_RGB)

    for zz <- 0 until zChunks do {
      for xx <- 0 until xChunks do {
        val heightMap = gen.getHeightmapInterpolator(ColumnRelWorld(xx, zz))
        for cz <- 0 until 16 do {
          val z = zz * 16 + cz
          for cx <- 0 until 16 do {
            val x = xx * 16 + cx
            val h = heightMap(cx, cz) + 128
            image.setRGB(x, z, clamp(h.toInt, 0, 255) << 8)
          }
        }
      }
    }

    ImageIO.write(image, "png", new File("heightmap.png"))
  }

  private def exportBiomeMap(xChunks: Int, zChunks: Int): Unit = {
    val image = BufferedImage(16 * xChunks, 16 * zChunks * 4, BufferedImage.TYPE_INT_RGB)

    for zz <- 0 until zChunks do {
      for xx <- 0 until xChunks do {
        val terrain = ChunkColumnTerrain.create(ColumnRelWorld(xx, zz), gen, None)

        val t = terrain.temperature.map(t => clamp((t * 128 + 128).toInt, 0, 255))
        val h = terrain.humidity.map(h => clamp((h * 128 + 128).toInt, 0, 255))

        val t5 = terrain.temperature.map(t => (t * 5).toInt / 5.0 * 128 + 128)
        val h5 = terrain.humidity.map(h => (h * 5).toInt / 5.0 * 128 + 128)

        for cz <- 0 until 16 do {
          val z = zz * 16 + cz
          for cx <- 0 until 16 do {
            val x = xx * 16 + cx

            for (col, idx) <- Seq(
                t(cx, cz).toInt << 16,
                h(cx, cz).toInt,
                t5(cx, cz).toInt << 16 | h5(cx, cz).toInt,
                biomeColor(terrain.biome(cx, cz))
              ).zipWithIndex
            do image.setRGB(x, z + zChunks * 16 * idx, col)
          }
        }
      }
    }

    ImageIO.write(image, "png", new File("biomes.png"))
  }

  private def biomeColor(biome: Biome): Int = biome match {
    case Biome.Desert     => 0xdddd22
    case Biome.Snowland   => 0xeeeeee
    case Biome.Rainforest => 0x22cc22
    case Biome.Tundra     => 0xaaaaaa
    case Biome.Grassland  => 0x000000
  }

  private def exportBlockMap(xChunks: Int, zChunks: Int): Unit = {
    val image = BufferedImage(16 * xChunks, 16 * zChunks, BufferedImage.TYPE_INT_RGB)

    val world = new BlocksInWorldExtended {
      private val columns = mutable.LongMap.empty[ChunkColumnTerrain]
      private val chunks = memoized[Long, Chunk] { k =>
        val coords = ChunkRelWorld(k)
        val terrain = this.provideColumn(coords.getColumnRelWorld)
        Chunk.fromGenerator(coords, terrain, gen)
      }

      override def provideColumn(coords: ColumnRelWorld): ChunkColumnTerrain = {
        columns.getOrElseUpdate(coords.value, ChunkColumnTerrain.create(coords, gen, None))
      }

      override def getColumn(coords: ColumnRelWorld): Option[ChunkColumnTerrain] =
        columns.get(coords.value)

      override def getChunk(coords: ChunkRelWorld): Option[Chunk] =
        Some(chunks(coords.value))

      override def getBlock(coords: BlockRelWorld): BlockState =
        getChunk(coords.getChunkRelWorld).get.getBlock(coords.getBlockRelChunk)
    }

    val planner = WorldPlanner(world, genSettings.seed)

    var prevProgress = 0

    Loop.rangeUntil(0, zChunks) { zz =>
      Loop.rangeUntil(0, xChunks) { xx =>
        val progress = (xx + zz * xChunks) * 100 / (xChunks * zChunks)
        if progress != prevProgress then {
          prevProgress = progress
          println(s"Generated blocks map: $progress%")
        }

        val colCoords = ColumnRelWorld(xx, zz)
        val terrain = world.provideColumn(ColumnRelWorld(xx, zz))

        val sY = terrain.terrainHeight.getHeight(8, 8) >> 4
        Loop.rangeTo(-16, 16) { dY =>
          planner.prepare(ChunkRelWorld(xx, sY + dY, zz))
        }

        Loop.rangeUntil(0, 16) { cz =>
          val z = zz * 16 + cz
          Loop.rangeUntil(0, 16) { cx =>
            val x = xx * 16 + cx
            terrain.terrainHeight.recalculate(
              BlockRelWorld(x, terrain.terrainHeight.getHeight(cx, cz) + 64, z),
              Y => {
                val coords = ChunkRelWorld(xx, Y, zz)
                planner.decorate(coords, world.getChunk(coords).get)
                world.getChunk(coords)
              }
            )
          }
        }

        Loop.rangeUntil(0, 16) { cz =>
          val z = zz * 16 + cz
          Loop.rangeUntil(0, 16) { cx =>
            val x = xx * 16 + cx

            val h = terrain.terrainHeight.getHeight(cx, cz).toInt
            val b = world.getChunk(ChunkRelWorld(xx, h >> 4, zz)).get.getBlock(BlockRelChunk(cx, h & 15, cz))
            val col = blockTextureColors.get(b.blockType.name).map(_.head).getOrElse(Vector3f(0, 0, 0))

            image.setRGB(x, z, colorFromFloats(col.x, col.y, col.z))
          }
        }
      }
    }

    ImageIO.write(image, "png", new File("blocks.png"))
  }

  private def fToI8(f: Float): Int = {
    clamp((f * 256).toInt, 0, 255)
  }

  private def colorFromFloats(r: Float, g: Float, b: Float): Int = {
    fToI8(r) << 16 | fToI8(g) << 8 | fToI8(b)
  }

  private def clamp(v: Int, lo: Int, hi: Int): Int = {
    if v < lo then lo else if v > hi then hi else v
  }

  private def memoized[A, B](f: A => B): A => B = {
    val cache = mutable.Map.empty[A, B]
    a => cache.getOrElseUpdate(a, f(a))
  }

  private def calculateTextureColor(texture: PixelArray): Vector3f = {
    var r = 0L
    var g = 0L
    var b = 0L

    for pix <- texture.pixels do {
      r += (pix >> 16) & 0xff
      g += (pix >> 8) & 0xff
      b += (pix >> 0) & 0xff
    }

    val c = texture.pixels.length * 255
    Vector3f(r.toFloat / c, g.toFloat / c, b.toFloat / c)
  }
}
