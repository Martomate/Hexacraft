package hexacraft.math.noise

import hexacraft.world.CylinderSize
import hexacraft.world.coord.fp.CylCoords

import java.util.Random

class NoiseGenerator4D(random: Random, val numOctaves: Int, val scale: Double) {
  private[this] val noiseGens = Seq.fill(numOctaves)(new SingleNoiseGen4D(random))

  def genNoise(x: Double, y: Double, z: Double, w: Double): Double = {
    var amp = 1d
    var result = 0d
    for (n <- noiseGens) {
      val mult = scale / amp
      result += amp * n.noise(x * mult, y * mult, z * mult, w * mult)
      amp /= 2
    }
    result
  }

  def genNoiseFromCyl(c: CylCoords)(using cylSize: CylinderSize): Double = {
    val angle = c.z / cylSize.radius
    genNoise(c.x, c.y, math.sin(angle) * cylSize.radius, math.cos(angle) * cylSize.radius)
  }
}
