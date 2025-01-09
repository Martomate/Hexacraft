package hexacraft.math.noise

import hexacraft.util.Loop

import java.util.Random

class NoiseGenerator3D(random: Random, val numOctaves: Int, val scale: Double) {
  private val noiseGens = Array.fill(numOctaves)(PerlinNoise3D(random))

  def genNoise(x: Double, y: Double, z: Double): Double = {
    var amp = 1d
    var result = 0d
    Loop.array(noiseGens) { n =>
      val mult = scale / amp
      val noise = n.noise(
        x * mult,
        y * mult,
        z * mult
      )
      result += amp * noise
      amp /= 2
    }
    result
  }

  def genWrappedNoise(x: Double, z: Double, radius: Double): Double = {
    val angle = z / radius
    genNoise(
      x,
      math.sin(angle) * radius,
      math.cos(angle) * radius
    )
  }
}
