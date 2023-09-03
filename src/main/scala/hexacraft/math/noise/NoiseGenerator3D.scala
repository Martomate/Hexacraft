package hexacraft.math.noise

import hexacraft.world.CylinderSize
import hexacraft.world.coord.fp.CylCoords

import java.util.Random

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

  def genNoiseFromCylXZ(c: CylCoords)(using cylSize: CylinderSize): Double = {
    val angle = c.z / cylSize.radius
    genNoise(c.x, math.sin(angle) * cylSize.radius, math.cos(angle) * cylSize.radius)
  }
}
