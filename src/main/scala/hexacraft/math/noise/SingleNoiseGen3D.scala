package hexacraft.math.noise

import org.joml.Math.triLerp

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

  private def grad(hash: Int, x: Double, y: Double, z: Double): Double = {
    val h = hash & 15
    val u = if (h < 8) x else y
    val v = if (h < 4) y else if (h == 12 || h == 14) x else z
    (if ((h & 1) == 0) u else -u) + (if ((h & 2) == 0) v else -v)
  }

  private def fade(t: Double): Double = t * t * t * (t * (t * 6 - 15) + 10)

  def noise(xx: Double, yy: Double, zz: Double): Double = {
    val (ix, x, tx) = intComps(xx)
    val (iy, y, ty) = intComps(yy)
    val (iz, z, tz) = intComps(zz)

    val q0 = perm(ix)
    val q1 = perm(ix + 1)

    val q00 = perm(q0 + iy)
    val q01 = perm(q0 + iy + 1)
    val q10 = perm(q1 + iy)
    val q11 = perm(q1 + iy + 1)

    val q000 = perm(q00 + iz)
    val q001 = perm(q10 + iz)
    val q010 = perm(q01 + iz)
    val q011 = perm(q11 + iz)
    val q100 = perm(q00 + iz + 1)
    val q101 = perm(q10 + iz + 1)
    val q110 = perm(q01 + iz + 1)
    val q111 = perm(q11 + iz + 1)

    triLerp(
      grad(q000, x - 0, y - 0, z - 0),
      grad(q001, x - 1, y - 0, z - 0),
      grad(q010, x - 0, y - 1, z - 0),
      grad(q011, x - 1, y - 1, z - 0),
      grad(q100, x - 0, y - 0, z - 1),
      grad(q101, x - 1, y - 0, z - 1),
      grad(q110, x - 0, y - 1, z - 1),
      grad(q111, x - 1, y - 1, z - 1),
      tx,
      ty,
      tz
    )
  }
}
