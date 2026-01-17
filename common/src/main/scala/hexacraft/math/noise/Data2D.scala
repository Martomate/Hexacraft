package hexacraft.math.noise

import hexacraft.math.Range2D
import hexacraft.util.Loop

import org.joml.Math.biLerp

case class Data2D(sizeX: Int, sizeY: Int, values: Array[Double]) {
  def apply(x: Int, y: Int): Double = {
    values(x + y * sizeX)
  }

  inline def map(inline f: Double => Double): Data2D = {
    Data2D.evaluate(sizeX, sizeY)((i, j) => f(this(i, j)))
  }

  override def equals(obj: Any) = obj match {
    case other: Data2D => this.values.sameElements(other.values)
    case _             => false
  }
}

object Data2D {
  inline def evaluate(indices: Range2D, inline fn: (Int, Int) => Double): Data2D = {
    val Range2D(xs, ys) = indices
    val values = new Array[Double](xs.length * ys.length)
    var idx = 0
    Loop.iterate(ys.iterator) { y =>
      Loop.iterate(xs.iterator) { x =>
        values(idx) = fn(x, y)
        idx += 1
      }
    }
    Data2D(xs.length, ys.length, values)
  }

  inline def evaluate(sizeX: Int, sizeY: Int)(inline fn: (Int, Int) => Double): Data2D = {
    val values = new Array[Double](sizeX * sizeY)
    var idx = 0
    Loop.rangeUntil(0, sizeY) { y =>
      Loop.rangeUntil(0, sizeX) { x =>
        values(idx) = fn(x, y)
        idx += 1
      }
    }
    Data2D(sizeX, sizeY, values)
  }

  def interpolate(scaleX: Int, scaleY: Int, data: Data2D): Data2D = {
    Data2D.evaluate(
      (data.sizeX - 1) * scaleX,
      (data.sizeY - 1) * scaleY
    ) { (x, y) =>
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
  }
}
