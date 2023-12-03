package hexacraft.math.noise

import hexacraft.math.Range2D

import org.joml.Math.biLerp

case class Data2D(sizeX: Int, sizeY: Int, values: Array[Double]) {
  def apply(x: Int, y: Int): Double =
    values(x + y * sizeX)
}

object Data2D {
  def evaluate(indices: Range2D, fn: (Int, Int) => Double): Data2D =
    val Range2D(xs, ys) = indices
    val values = for (y <- ys; x <- xs) yield fn(x, y)
    Data2D(xs.length, ys.length, values.toArray)

  def interpolate(scaleX: Int, scaleY: Int, data: Data2D): Data2D =
    val xs = 0 until (data.sizeX - 1) * scaleX
    val ys = 0 until (data.sizeY - 1) * scaleY

    Data2D.evaluate(
      Range2D(xs, ys),
      (x, y) => {
        val ii = x / scaleX
        val ij = y / scaleY
        val fi = (x % scaleX) / scaleX.toDouble
        val fj = (y % scaleY) / scaleY.toDouble

        biLerp(
          data(ii, ij),
          data(ii, ij + 1),
          data(ii + 1, ij),
          data(ii + 1, ij + 1),
          fj,
          fi
        )
      }
    )

}
