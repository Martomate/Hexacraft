package hexacraft.math.noise

import hexacraft.world.CylinderSize
import hexacraft.world.coord.fp.CylCoords

import munit.FunSuite

import java.util.Random

class NoiseGenerator4DTest extends FunSuite {
  test("the noise should be fixed for the same input") {
    val seed = 123456789L
    val gen = makeGen(seed)
    val gen2 = makeGen(seed)

    // These assertions act as regression tests
    assertEqualsDouble(gen.genNoise(0.1, 0.2, 0.3, 0.4321), -0.006333065288690849, 1e-15)
    assertEqualsDouble(gen.genNoise(-0.1, 0.2, 0.3, 0.3214), -0.005216287827631798, 1e-15)
    assertEqualsDouble(gen.genNoise(0.1234, -0.2, 0.3, 0.4), -0.002016292746646677, 1e-15)
    assertEqualsDouble(gen.genNoise(0.2345, 0.2, -0.3, 0.4), -0.006003508521853744, 1e-15)
  }

  test("noise should be the same for the same input") {
    val rand = new Random
    val seed = rand.nextLong
    val gen = makeGen(seed)
    val gen2 = makeGen(seed)

    val scale = 100
    for (_ <- 1 to 10) {
      val (x, y, z, w) = nextDouble4(rand, scale)

      assert(gen.genNoise(x, y, z, w) == gen2.genNoise(x, y, z, w))
    }
  }

  test("noise should not be constant") {
    val rand = new Random
    val gen = makeGen(rand.nextLong)

    val scale = 100

    val values = for (_ <- 1 to 10) yield {
      val (x, y, z, w) = nextDouble4(rand, scale)

      gen.genNoise(x, y, z, w)
    }

    assert(values.toSet.size > 1)
  }

  test("genNoiseFromCyl should be correct") {
    val rand = new Random
    val gen = makeGen(rand.nextLong)

    val size = CylinderSize(5)
    import size.impl
    val scale = 100
    for (_ <- 1 to 10) {
      val cyl = CylCoords(nextDouble(rand, scale), nextDouble(rand, scale), nextDouble(rand, scale))
      val angle = cyl.z / size.radius

      val noiseFromCyl = gen.genNoiseFromCyl(cyl)
      val expectedNoise = gen.genNoise(cyl.x, cyl.y, math.sin(angle) * size.radius, math.cos(angle) * size.radius)

      assertEquals(noiseFromCyl, expectedNoise)
    }
  }

  private def makeGen(seed: Long, octaves: Int = 4, scale: Double = 0.01): NoiseGenerator4D = {
    new NoiseGenerator4D(new Random(seed), octaves, scale)
  }

  private def nextDouble(rand: Random, scale: Int) = {
    rand.nextDouble * scale
  }

  private def nextDouble4(rand: Random, scale: Int): (Double, Double, Double, Double) = {
    val x = nextDouble(rand, scale)
    val y = nextDouble(rand, scale)
    val z = nextDouble(rand, scale)
    val w = nextDouble(rand, scale)
    (x, y, z, w)
  }

}
