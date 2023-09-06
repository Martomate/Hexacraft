package hexacraft.math.noise

import hexacraft.world.CylinderSize
import hexacraft.world.coord.fp.CylCoords

import munit.FunSuite

import java.util.Random

class NoiseGenerator3DTest extends FunSuite {
  test("the noise should be fixed for the same input") {
    val seed = 123456789L
    val gen = makeGen(seed)
    val gen2 = makeGen(seed)

    // These assertions act as regression tests
    assertEqualsDouble(gen.genNoise(0.1, 0.2, 0.3), -0.008011387068078604, 1e-15)
    assertEqualsDouble(gen.genNoise(-0.1, 0.2, 0.3), -0.012011955862618138, 1e-15)
    assertEqualsDouble(gen.genNoise(0.1234, -0.2, 0.3), 4.6908453063207456E-4, 1e-15)
    assertEqualsDouble(gen.genNoise(0.2345, 0.2, -0.3), 0.006672026046915622, 1e-15)
  }

  test("the noise should be the same for the same input") {
    val rand = new Random
    val seed = rand.nextLong
    val gen = makeGen(seed)
    val gen2 = makeGen(seed)

    val scale = 100
    for (_ <- 1 to 10) {
      val (x, y, z) = nextDouble3(rand, scale)

      assert(gen.genNoise(x, y, z) == gen2.genNoise(x, y, z))
    }
  }

  test("the noise should not be constant") {
    val rand = new Random
    val gen = makeGen(rand.nextLong)

    val scale = 100

    val values = for (_ <- 1 to 10) yield {
      val (x, y, z) = nextDouble3(rand, scale)

      gen.genNoise(x, y, z)
    }

    assert(values.toSet.size > 1)
  }

  test("genNoiseFromCylXZ should be correct") {
    val rand = new Random
    val gen = makeGen(rand.nextLong)

    val size = CylinderSize(5)
    import size.impl
    val scale = 100
    for (_ <- 1 to 10) {
      val cyl = CylCoords(nextDouble(rand, scale), nextDouble(rand, scale), nextDouble(rand, scale))
      val angle = cyl.z / size.radius

      val noiseFromCyl = gen.genNoiseFromCylXZ(cyl)
      val expectedNoise = gen.genNoise(cyl.x, math.sin(angle) * size.radius, math.cos(angle) * size.radius)

      assertEquals(noiseFromCyl, expectedNoise)
    }
  }

  private def makeGen(seed: Long, octaves: Int = 4, scale: Double = 0.01): NoiseGenerator3D = {
    new NoiseGenerator3D(new Random(seed), octaves, scale)
  }

  private def nextDouble(rand: Random, scale: Int) = {
    rand.nextDouble * scale
  }

  private def nextDouble3(rand: Random, scale: Int): (Double, Double, Double) = {
    val x = nextDouble(rand, scale)
    val y = nextDouble(rand, scale)
    val z = nextDouble(rand, scale)
    (x, y, z)
  }

}
