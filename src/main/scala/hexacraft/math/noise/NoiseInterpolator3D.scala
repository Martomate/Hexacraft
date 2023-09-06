package hexacraft.math.noise

import org.joml.Math.triLerp

class NoiseInterpolator3D(xSize: Int, ySize: Int, zSize: Int, sampler: (Int, Int, Int) => Double) {
  private val noiseSamples: Array[Double] =
    (for (i <- 0 to xSize; j <- 0 to ySize; k <- 0 to zSize) yield sampler(i, j, k)).toArray
  private val stride2: Int = (ySize + 1) * (zSize + 1)
  private val stride1: Int = zSize + 1

  private def interpolate(ii: Int, ij: Int, ik: Int, fi: Double, fj: Double, fk: Double): Double = {
    triLerp(
      noiseSamples(ii * stride2 + ij * stride1 + ik),
      noiseSamples(ii * stride2 + ij * stride1 + ik + 1),
      noiseSamples(ii * stride2 + (ij + 1) * stride1 + ik),
      noiseSamples(ii * stride2 + (ij + 1) * stride1 + ik + 1),
      noiseSamples((ii + 1) * stride2 + ij * stride1 + ik),
      noiseSamples((ii + 1) * stride2 + ij * stride1 + ik + 1),
      noiseSamples((ii + 1) * stride2 + (ij + 1) * stride1 + ik),
      noiseSamples((ii + 1) * stride2 + (ij + 1) * stride1 + ik + 1),
      fk,
      fj,
      fi
    )
  }

  def apply(x: Int, y: Int, z: Int): Double = interpolate(
    x / xSize,
    y / ySize,
    z / zSize,
    (x % xSize) / xSize.toDouble,
    (y % ySize) / ySize.toDouble,
    (z % zSize) / zSize.toDouble
  )
}
