package hexacraft.math.noise

import org.joml.Math.biLerp

class NoiseInterpolator2D(iSize: Int, jSize: Int, sampler: (Int, Int) => Double) {
  private val noiseSamples = for (i <- 0 to iSize; j <- 0 to jSize) yield sampler(i, j)
  private val stride = jSize + 1

  private def interpolate(ii: Int, ij: Int, fi: Double, fj: Double): Double = {
    biLerp(
      noiseSamples(ii * stride + ij),
      noiseSamples(ii * stride + ij + 1),
      noiseSamples((ii + 1) * stride + ij),
      noiseSamples((ii + 1) * stride + ij + 1),
      fj,
      fi
    )
  }

  def apply(i: Int, j: Int): Double =
    interpolate(i / iSize, j / jSize, (i % iSize) / iSize.toDouble, (j % jSize) / jSize.toDouble)
}
