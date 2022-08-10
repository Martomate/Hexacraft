package com.martomate.hexacraft.world.gen.noise

class NoiseInterpolator2D(iSize: Int, jSize: Int, sampler: (Int, Int) => Double) {
  private val noiseSamples = for (i <- 0 to iSize; j <- 0 to jSize) yield sampler(i, j)
  private val stride = jSize + 1

  private def lerp(t: Double, v1: Double, v2: Double) = v1 + t * (v2 - v1)

  private def interpolate(ii: Int, ij: Int, fi: Double, fj: Double): Double = {
    lerp(
      fi,
      lerp(fj, noiseSamples(ii * stride + ij), noiseSamples(ii * stride + ij + 1)),
      lerp(fj, noiseSamples((ii + 1) * stride + ij), noiseSamples((ii + 1) * stride + ij + 1))
    )
  }

  def apply(i: Int, j: Int): Double =
    interpolate(i / iSize, j / jSize, (i % iSize) / iSize.toDouble, (j % jSize) / jSize.toDouble)
}
