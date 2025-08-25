package hexacraft.math.noise

import hexacraft.rs.RustLib
import hexacraft.util.Loop
import hexacraft.util.SeqUtils.shuffleArray

import java.util.Random

class NoiseGenerator4D(random: Random, val numOctaves: Int, val scale: Double) {
  private val noiseGens = Array.fill(numOctaves) {
    val arr = (0 until 256).toArray
    shuffleArray(arr, random)
    val perm = arr ++ arr
    RustLib.PerlinNoise4D.init(perm)
  }

  def genNoise(x: Double, y: Double, z: Double, w: Double): Double = {
    var amp = 1d
    var result = 0d
    Loop.array(noiseGens) { n =>
      val mult = scale / amp
      val noise = RustLib.PerlinNoise4D.noise(
        n,
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
