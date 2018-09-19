package com.martomate.hexacraft.world.gen.noise

import java.util.Random

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.coord.fp.CylCoords
import org.scalatest.FunSuite

class NoiseGenerator3DTest extends FunSuite {
  test("same input should give same output") {
    val rand = new Random
    val seed = rand.nextLong
    val gen = makeGen(seed)
    val gen2 = makeGen(seed)

    val scale = 100
    for (_ <- 1 to 10) {
      val (x, y, z) = nextDouble3(rand, scale)

      assertResult(gen.genNoise(x, y, z))(gen2.genNoise(x, y, z))
    }
  }

  test("noise should not be constant") {
    val rand = new Random
    val gen = makeGen(rand.nextLong)

    val scale = 100
    val result = gen.genNoise(0, 0, 0)

    val different = (1 to 10).exists(_ => {
      val (x, y, z) = nextDouble3(rand, scale)

      result != gen.genNoise(x, y, z)
    })

    assert(different)
  }

  test("genNoiseFromCylXZ is correct") {
    val rand = new Random
    val gen = makeGen(rand.nextLong)

    val size = new CylinderSize(5)
    val scale = 100
    for (_ <- 1 to 10) {
      val cyl = CylCoords(nextDouble(rand, scale), nextDouble(rand, scale), nextDouble(rand, scale), size)
      val angle = cyl.z / size.radius
      assertResult(gen.genNoise(cyl.x, math.sin(angle) * size.radius, math.cos(angle) * size.radius))(gen.genNoiseFromCylXZ(cyl))
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
