package com.martomate.hexacraft.world.gen.noise

import java.util.Random

// Improved Perlin Noise: http://mrl.nyu.edu/~perlin/noise/
class SingleNoiseGen3D(random: Random) { // Apparently SimplexNoise exists in joml
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

  private def grad(hash: Int, x: Double, y: Double, z: Double): Double = {
    val h = hash & 15
    val u = if (h < 8) x else y
    val v = if (h < 4) y else if (h == 12 || h == 14) x else z
    (if ((h & 1) == 0) u else -u) + (if ((h & 2) == 0) v else -v)
  }

  private def fade(t: Double): Double = t * t * t * (t * (t * 6 - 15) + 10)

  def noise(xx: Double, yy: Double, zz: Double): Double = {
    val (ix, x, u) = intComps(xx)
    val (iy, y, v) = intComps(yy)
    val (iz, z, w) = intComps(zz)

    val a = perm(ix)
    val b = perm(ix + 1)
    val aa = perm(a + iy) + iz
    val ab = perm(a + iy + 1) + iz
    val ba = perm(b + iy) + iz
    val bb = perm(b + iy + 1) + iz

    lerp(
      w,
      lerp(
        v,
        lerp(u, grad(perm(aa), x, y, z), grad(perm(ba), x - 1, y, z)),
        lerp(u, grad(perm(ab), x, y - 1, z), grad(perm(bb), x - 1, y - 1, z))
      ),
      lerp(
        v,
        lerp(u, grad(perm(aa + 1), x, y, z - 1), grad(perm(ba + 1), x - 1, y, z - 1)),
        lerp(u, grad(perm(ab + 1), x, y - 1, z - 1), grad(perm(bb + 1), x - 1, y - 1, z - 1))
      )
    )
  }
}
