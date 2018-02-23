package hexagon.world.gen.noise

import java.util.Random
import hexagon.world.storage.World
import hexagon.world.coord.CylCoords

class NoiseGenerator3D(random: Random, val numOctaves: Int, val scale: Double) {
  private[this] val noiseGens = Seq.fill(numOctaves)(new SingleNoiseGen3D(random))

  def genNoise(x: Double, y: Double, z: Double): Double = {
    var amp = 1d
    var result = 0d
    for (n <- noiseGens) {
      val mult = scale / amp
      result += amp * n.noise(x * mult, y * mult, z * mult)
      amp /= 2
    }
    result
  }

  def genNoiseFromCylXZ(c: CylCoords): Double = {
    val angle = c.z / c.world.radius
    genNoise(c.x, math.sin(angle) * c.world.radius, math.cos(angle) * c.world.radius)
  }
}
