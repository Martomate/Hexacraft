package hexacraft.math.noise

import java.util.Random

class NoiseGenerator4D(random: Random, val numOctaves: Int, val scale: Double) {
  private val noiseGens = Seq.fill(numOctaves)(PerlinNoise4D(random))

  def genNoise(x: Double, y: Double, z: Double, w: Double): Double = {
    var amp = 1d
    var result = 0d
    for n <- noiseGens do {
      val mult = scale / amp
      val noise = n.noise(
        x * mult,
        y * mult,
        z * mult,
        w * mult
      )
      result += amp * noise
      amp /= 2
    }
    result
  }

  def genWrappedNoise(x: Double, y: Double, z: Double, radius: Double): Double = {
    val angle = z / radius
    genNoise(
      x,
      y,
      math.sin(angle) * radius,
      math.cos(angle) * radius
    )
  }
}
