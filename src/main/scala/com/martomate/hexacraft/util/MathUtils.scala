package com.martomate.hexacraft.util

object MathUtils {
  def fitZ(z: Double, circumference: Double): Double = {
    val zz = z % circumference
    if (zz < 0) {
      zz + circumference
    } else {
      zz
    }
  }

  /** @return x or (x - C) depending on which one is closest to 0 on the modulo circle */
  def absmin(x: Double, circumference: Double): Double = {
    fitZ(x + circumference / 2, circumference) - circumference / 2
  }

  /** Linear interpolation with `t` ranging from 0 to 1 */
  def lerp(t: Float, start: Float, end: Float): Float =
    start + (end - start) * t

  /** Remaps the range [`loIn`, `hiIn`] to [`loOut`, `hiOut`] sampled at `t` */
  def remap(t: Float, loIn: Float, hiIn: Float, loOut: Float, hiOut: Float): Float =
    lerp((t - loIn) / (hiIn - loIn), loOut, hiOut)

  /** @return `value` unless it is lower than `lo` or higher than `hi` */
  def clamp(value: Float, lo: Float, hi: Float): Float =
    if (value < lo) lo else if (value > hi) hi else value
}
