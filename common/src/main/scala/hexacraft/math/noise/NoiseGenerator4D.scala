package hexacraft.math.noise

import hexacraft.rs.RustLib
import hexacraft.util.SeqUtils.shuffleArray

import java.util.Random

class NoiseGenerator4D(random: Random, val numOctaves: Int, val scale: Double) {
  private val perms = Array.fill(numOctaves) {
    val arr = (0 until 256).toArray
    shuffleArray(arr, random)
    val perm = arr ++ arr
    RustLib.NoiseGenerator4D.storePerms(perm)
  }

  private val handle = RustLib.NoiseGenerator4D.createLayeredNoiseGenerator(perms)

  def genNoise(x: Double, y: Double, z: Double, w: Double): Double = {
    RustLib.NoiseGenerator4D.genNoise(handle, scale, x, y, z, w)
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
