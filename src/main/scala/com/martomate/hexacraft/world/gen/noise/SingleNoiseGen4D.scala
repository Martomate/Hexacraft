package com.martomate.hexacraft.world.gen.noise

import java.util.Random

// Improved Perlin Noise: http://mrl.nyu.edu/~perlin/noise/
class SingleNoiseGen4D(random: Random) { // Apparently SimplexNoise exists in joml
  private val grad4 = Array(0, 1, 1, 1, 0, 1, 1, -1, 0, 1, -1, 1, 0, 1, -1, -1, 0, -1, 1, 1, 0, -1, 1, -1, 0, -1, -1, 1,
    0, -1, -1, -1, 1, 0, 1, 1, 1, 0, 1, -1, 1, 0, -1, 1, 1, 0, -1, -1, -1, 0, 1, 1, -1, 0, 1, -1, -1, 0, -1, 1, -1, 0,
    -1, -1, 1, 1, 0, 1, 1, 1, 0, -1, 1, -1, 0, 1, 1, -1, 0, -1, -1, 1, 0, 1, -1, 1, 0, -1, -1, -1, 0, 1, -1, -1, 0, -1,
    1, 1, 1, 0, 1, 1, -1, 0, 1, -1, 1, 0, 1, -1, -1, 0, -1, 1, 1, 0, -1, 1, -1, 0, -1, -1, 1, 0, -1, -1, -1, 0)

  private[this] val perm = {
    val arr = (0 until 256).toArray
    for (i <- arr.indices) {
      val idx = random.nextInt(256 - i) + i
      val temp = arr(i)
      arr(i) = arr(idx)
      arr(idx) = temp
    }
    arr ++ arr
  }

  private def intComps(i: Double): (Int, Double, Double) = {
    val intPart = math.floor(i).toInt
    val rest = i - intPart
    (intPart & 255, rest, fade(rest))
  }

  private def lerp(t: Double, a: Double, b: Double): Double = a + t * (b - a)

  private def grad(hash: Int, x: Double, y: Double, z: Double, w: Double): Double = {
    val h = hash & 31
    grad4(h * 4 + 0) * x + grad4(h * 4 + 1) * y + grad4(h * 4 + 2) * z + grad4(h * 4 + 3) * w
  }

  private def fade(t: Double): Double = t * t * t * (t * (t * 6 - 15) + 10)

  def noise(xx: Double, yy: Double, zz: Double, ww: Double): Double = {
    val (ix1, x1, u1) = intComps(xx)
    val (ix2, x2, u2) = intComps(yy)
    val (ix3, x3, u3) = intComps(zz)
    val (ix4, x4, u4) = intComps(ww)

    def lerpX(hash: Int, y: Double, z: Double, w: Double) =
      lerp(u1, grad(perm(hash + ix1), x1, y, z, w), grad(perm(hash + ix1 + 1), x1 - 1, y, z, w))
    def lerpY(hash: Int, z: Double, w: Double) =
      lerp(u2, lerpX(perm(hash + ix2), x2, z, w), lerpX(perm(hash + ix2 + 1), x2 - 1, z, w))
    def lerpZ(hash: Int, w: Double) =
      lerp(u3, lerpY(perm(hash + ix3), x3, w), lerpY(perm(hash + ix3 + 1), x3 - 1, w))
    def lerpW =
      lerp(u4, lerpZ(perm(ix4), x4), lerpZ(perm(ix4 + 1), x4 - 1))
    lerpW
  }
}
