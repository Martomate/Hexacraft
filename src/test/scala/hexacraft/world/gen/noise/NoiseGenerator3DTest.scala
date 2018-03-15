package hexacraft.world.gen.noise

import java.util.Random

import hexacraft.world.coord.CylCoords
import hexacraft.world.storage.CylinderSize
import org.scalatest.FunSuite

class NoiseGenerator3DTest extends FunSuite {
  test("same input should give same output") {
    val rand = new Random
    val seed = rand.nextLong
    val gen = new NoiseGenerator3D(new Random(seed), 4, 0.01)
    val gen2 = new NoiseGenerator3D(new Random(seed), 4, 0.01)

    val scale = 100
    for (_ <- 1 to 10) {
      val x = rand.nextDouble * scale
      val y = rand.nextDouble * scale
      val z = rand.nextDouble * scale
      assertResult(gen.genNoise(x, y, z))(gen2.genNoise(x, y, z))
    }
  }

  test("noise should not be constant") {
    val rand = new Random
    val gen = new NoiseGenerator3D(new Random(rand.nextLong), 4, 0.01)

    val scale = 100
    var result = gen.genNoise(0, 0, 0)

    val different = (1 to 10).exists(_ => {
      val x = rand.nextDouble * scale
      val y = rand.nextDouble * scale
      val z = rand.nextDouble * scale

      result != gen.genNoise(x, y, z)
    })

    assert(different)
  }

  test("genNoiseFromCylXZ is correct") {
    val rand = new Random
    val gen = new NoiseGenerator3D(new Random(rand.nextLong), 4, 0.01)

    val size = new CylinderSize(5)
    val scale = 100
    for (_ <- 1 to 10) {
      val cyl = CylCoords(rand.nextDouble * scale, rand.nextDouble * scale, rand.nextDouble * scale, size)
      val angle = cyl.z / size.radius
      assertResult(gen.genNoise(cyl.x, math.sin(angle) * size.radius, math.cos(angle) * size.radius))(gen.genNoiseFromCylXZ(cyl))
    }
  }
}
