package com.martomate.hexacraft.world.gen.noise

import java.util.Random

import com.martomate.hexacraft.world.CylinderSize
import com.martomate.hexacraft.world.coord.CylCoords
import org.scalatest.FunSuite

class NoiseGenerator4DTest extends FunSuite {
  test("same input should give same output") {
    val rand = new Random
    val seed = rand.nextLong
    val gen = new NoiseGenerator4D(new Random(seed), 4, 0.01)
    val gen2 = new NoiseGenerator4D(new Random(seed), 4, 0.01)

    val scale = 100
    for (_ <- 1 to 10) {
      val x = rand.nextDouble * scale
      val y = rand.nextDouble * scale
      val z = rand.nextDouble * scale
      val w = rand.nextDouble * scale
      assertResult(gen.genNoise(x, y, z, w))(gen2.genNoise(x, y, z, w))
    }
  }

  test("noise should not be constant") {
    val rand = new Random
    val gen = new NoiseGenerator4D(new Random(rand.nextLong), 4, 0.01)

    val scale = 100
    val result = gen.genNoise(0, 0, 0, 0)

    val different = (1 to 10).exists(_ => {
      val x = rand.nextDouble * scale
      val y = rand.nextDouble * scale
      val z = rand.nextDouble * scale
      val w = rand.nextDouble * scale

      result != gen.genNoise(x, y, z, w)
    })

    assert(different)
  }

  test("genNoiseFromCyl is correct") {
    val rand = new Random
    val gen = new NoiseGenerator4D(new Random(rand.nextLong), 4, 0.01)

    val size = new CylinderSize(5)
    val scale = 100
    for (_ <- 1 to 10) {
      val cyl = CylCoords(rand.nextDouble * scale, rand.nextDouble * scale, rand.nextDouble * scale, size)
      val angle = cyl.z / size.radius
      assertResult(gen.genNoise(cyl.x, cyl.y, math.sin(angle) * size.radius, math.cos(angle) * size.radius))(gen.genNoiseFromCyl(cyl))
    }
  }
}
