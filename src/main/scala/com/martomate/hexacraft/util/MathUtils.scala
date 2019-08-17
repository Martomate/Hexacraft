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
}
