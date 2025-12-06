package hexacraft.math.noise

import hexacraft.rs.RustLib
import hexacraft.util.SeqUtils.shuffleArray

import java.util.Random

class NoiseGenerator3D(random: Random, val numOctaves: Int, val scale: Double) {
  private val perms = Array.fill(numOctaves) {
    val arr = (0 until 256).toArray
    shuffleArray(arr, random)
    val perm = arr ++ arr
    RustLib.NoiseGenerator3D.storePerms(perm)
  }

  private val handle = RustLib.NoiseGenerator3D.createLayeredNoiseGenerator(perms)

  def genNoise(x: Double, y: Double, z: Double): Double = {
    RustLib.NoiseGenerator3D.genNoise(handle, scale, x, y, z)
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
