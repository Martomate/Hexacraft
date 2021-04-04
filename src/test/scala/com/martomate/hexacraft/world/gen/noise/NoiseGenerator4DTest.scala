package com.martomate.hexacraft.world.gen.noise

import java.util.Random
import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.coord.fp.CylCoords
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class NoiseGenerator4DTest extends AnyFlatSpec with Matchers {
  "noise" should "be the same for the same input" in {
    val rand = new Random
    val seed = rand.nextLong
    val gen = makeGen(seed)
    val gen2 = makeGen(seed)

    val scale = 100
    for (_ <- 1 to 10) {
      val (x, y, z, w) = nextDouble4(rand, scale)

      gen.genNoise(x, y, z, w) shouldBe gen2.genNoise(x, y, z, w)
    }
  }

  it should "not be constant" in {
    val rand = new Random
    val gen = makeGen(rand.nextLong)

    val scale = 100

    val values = for (_ <- 1 to 10) yield {
      val (x, y, z, w) = nextDouble4(rand, scale)

      gen.genNoise(x, y, z, w)
    }

    values.toSet.size should be > 1
  }

  "genNoiseFromCyl" should "be correct" in {
    val rand = new Random
    val gen = makeGen(rand.nextLong)

    val size = new CylinderSize(5)
    import size.impl
    val scale = 100
    for (_ <- 1 to 10) {
      val cyl = CylCoords(nextDouble(rand, scale), nextDouble(rand, scale), nextDouble(rand, scale))
      val angle = cyl.z / size.radius

      val noiseFromCyl = gen.genNoiseFromCyl(cyl)
      val expectedNoise = gen.genNoise(cyl.x, cyl.y, math.sin(angle) * size.radius, math.cos(angle) * size.radius)

      noiseFromCyl shouldBe expectedNoise
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
