package hexacraft.math.noise

import hexacraft.util.SeqUtils.shuffleArray

import org.joml.Math.{lerp, triLerp}

import java.util.Random

// Improved Perlin Noise: http://mrl.nyu.edu/~perlin/noise/
class PerlinNoise4D(random: Random) { // Apparently SimplexNoise exists in joml
  // format: off
  private val grad4 = Array(
     0,  1,  1,  1,    0,  1,  1, -1,    0,  1, -1,  1,    0,  1, -1, -1,
     0, -1,  1,  1,    0, -1,  1, -1,    0, -1, -1,  1,    0, -1, -1, -1,
     1,  0,  1,  1,    1,  0,  1, -1,    1,  0, -1,  1,    1,  0, -1, -1,
    -1,  0,  1,  1,   -1,  0,  1, -1,   -1,  0, -1,  1,   -1,  0, -1, -1,
     1,  1,  0,  1,    1,  1,  0, -1,    1, -1,  0,  1,    1, -1,  0, -1,
    -1,  1,  0,  1,   -1,  1,  0, -1,   -1, -1,  0,  1,   -1, -1,  0, -1,
     1,  1,  1,  0,    1,  1, -1,  0,    1, -1,  1,  0,    1, -1, -1,  0,
    -1,  1,  1,  0,   -1,  1, -1,  0,   -1, -1,  1,  0,   -1, -1, -1,  0
  )
  // format: on

  private[this] val perm = {
    val arr = (0 until 256).toArray
    shuffleArray(arr, random)
    arr ++ arr
  }

  private def intComps(i: Double): (Int, Double, Double) = {
    val intPart = math.floor(i).toInt
    val rest = i - intPart
    (intPart & 255, rest, fade(rest))
  }

  private def grad(hash: Int, x: Double, y: Double, z: Double, w: Double): Double = {
    val h = hash & 31
    grad4(h * 4 + 0) * x + grad4(h * 4 + 1) * y + grad4(h * 4 + 2) * z + grad4(h * 4 + 3) * w
  }

  private def fade(t: Double): Double = t * t * t * (t * (t * 6 - 15) + 10)

  def noise(xx: Double, yy: Double, zz: Double, ww: Double): Double = {
    val (ix, x, tx) = intComps(xx)
    val (iy, y, ty) = intComps(yy)
    val (iz, z, tz) = intComps(zz)
    val (iw, w, tw) = intComps(ww)

    val a0 = perm(iw)
    val a1 = perm(iw + 1)

    val a00 = perm(a0 + iz)
    val a01 = perm(a0 + iz + 1)
    val a10 = perm(a1 + iz)
    val a11 = perm(a1 + iz + 1)

    val a000 = perm(a00 + iy)
    val a001 = perm(a00 + iy + 1)
    val a010 = perm(a01 + iy)
    val a011 = perm(a01 + iy + 1)
    val a100 = perm(a10 + iy)
    val a101 = perm(a10 + iy + 1)
    val a110 = perm(a11 + iy)
    val a111 = perm(a11 + iy + 1)

    val a0000 = perm(a000 + ix)
    val a0001 = perm(a000 + ix + 1)
    val a0010 = perm(a001 + ix)
    val a0011 = perm(a001 + ix + 1)
    val a0100 = perm(a010 + ix)
    val a0101 = perm(a010 + ix + 1)
    val a0110 = perm(a011 + ix)
    val a0111 = perm(a011 + ix + 1)
    val a1000 = perm(a100 + ix)
    val a1001 = perm(a100 + ix + 1)
    val a1010 = perm(a101 + ix)
    val a1011 = perm(a101 + ix + 1)
    val a1100 = perm(a110 + ix)
    val a1101 = perm(a110 + ix + 1)
    val a1110 = perm(a111 + ix)
    val a1111 = perm(a111 + ix + 1)

    lerp(
      triLerp(
        grad(a0000, x - 0, y - 0, z - 0, w - 0),
        grad(a0001, x - 1, y - 0, z - 0, w - 0),
        grad(a0010, x - 0, y - 1, z - 0, w - 0),
        grad(a0011, x - 1, y - 1, z - 0, w - 0),
        grad(a0100, x - 0, y - 0, z - 1, w - 0),
        grad(a0101, x - 1, y - 0, z - 1, w - 0),
        grad(a0110, x - 0, y - 1, z - 1, w - 0),
        grad(a0111, x - 1, y - 1, z - 1, w - 0),
        tx,
        ty,
        tz
      ),
      triLerp(
        grad(a1000, x - 0, y - 0, z - 0, w - 1),
        grad(a1001, x - 1, y - 0, z - 0, w - 1),
        grad(a1010, x - 0, y - 1, z - 0, w - 1),
        grad(a1011, x - 1, y - 1, z - 0, w - 1),
        grad(a1100, x - 0, y - 0, z - 1, w - 1),
        grad(a1101, x - 1, y - 0, z - 1, w - 1),
        grad(a1110, x - 0, y - 1, z - 1, w - 1),
        grad(a1111, x - 1, y - 1, z - 1, w - 1),
        tx,
        ty,
        tz
      ),
      tw
    )
  }
}
