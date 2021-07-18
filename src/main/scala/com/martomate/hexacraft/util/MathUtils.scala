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
}
