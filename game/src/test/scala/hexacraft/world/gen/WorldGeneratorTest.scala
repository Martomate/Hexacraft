package hexacraft.world.gen

import hexacraft.world.{CylinderSize, WorldGenerator}
import hexacraft.world.coord.{ChunkRelWorld, ColumnRelWorld}
import munit.FunSuite

class WorldGeneratorTest extends FunSuite {
  given CylinderSize = CylinderSize(8)

  test("block interpolator works") {
    val fn: (Int, Int, Int) => Double = (x, y, z) => x + 3 * y + 5 * z
    val sampler = WorldGenerator.makeBlockInterpolator(ChunkRelWorld(1, 2, 3), fn)
    for
      x <- 0 until 16
      y <- 0 until 16
      z <- 0 until 16
    do assertEqualsDouble(sampler(x, y, z), fn(16 + x, 32 + y, 48 + z), 1e-6, clue = (x, y, z))
  }

  test("heightmap interpolator works") {
    val fn: (Int, Int) => Double = (x, z) => x + 5 * z
    val sampler = WorldGenerator.makeHeightmapInterpolator(ColumnRelWorld(1, 3), fn)
    for
      x <- 0 until 16
      y <- 0 until 16
      z <- 0 until 16
    do assertEqualsDouble(sampler(x, z), fn(16 + x, 48 + z), 1e-6, clue = (x, y, z))
  }
}
