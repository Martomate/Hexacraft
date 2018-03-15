package hexacraft.world.gen.noise

import java.util.Random
import hexacraft.world.storage.World
import hexacraft.world.coord.CylCoords

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
    val angle = c.z / c.cylSize.radius
    genNoise(c.x, math.sin(angle) * c.cylSize.radius, math.cos(angle) * c.cylSize.radius)
  }
}
