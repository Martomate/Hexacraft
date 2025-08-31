package hexacraft.math.noise

import hexacraft.math.Range3D
import hexacraft.util.Loop

import org.joml.Math.triLerp

case class Data3D(sizeX: Int, sizeY: Int, sizeZ: Int, values: Array[Double]) {
  def apply(x: Int, y: Int, z: Int): Double = {
    values(x + y * sizeX + z * sizeX * sizeY)
  }
}

object Data3D {
  inline def evaluate(indices: Range3D, inline fn: (Int, Int, Int) => Double): Data3D = {
    val Range3D(xs, ys, zs) = indices
    val values = new Array[Double](xs.length * ys.length * zs.length)
    var idx = 0
    Loop.iterate(zs.iterator) { z =>
      Loop.iterate(ys.iterator) { y =>
        Loop.iterate(xs.iterator) { x =>
          values(idx) = fn(x, y, z)
          idx += 1
        }
      }
    }
    Data3D(xs.length, ys.length, zs.length, values)
  }

  inline def evaluate(sizeX: Int, sizeY: Int, sizeZ: Int)(inline fn: (Int, Int, Int) => Double): Data3D = {
    val values = new Array[Double](sizeX * sizeY * sizeZ)
    var idx = 0
    Loop.rangeUntil(0, sizeZ) { z =>
      Loop.rangeUntil(0, sizeY) { y =>
        Loop.rangeUntil(0, sizeX) { x =>
          values(idx) = fn(x, y, z)
          idx += 1
        }
      }
    }
    Data3D(sizeX, sizeY, sizeZ, values)
  }

  def interpolate(scaleX: Int, scaleY: Int, scaleZ: Int, data: Data3D): Data3D = {
    Data3D.evaluate(
      (data.sizeX - 1) * scaleX,
      (data.sizeY - 1) * scaleY,
      (data.sizeZ - 1) * scaleZ
    ) { (x, y, z) =>
      val ii = x / scaleX
      val ij = y / scaleY
      val ik = z / scaleZ
      val fi = (x % scaleX) / scaleX.toDouble
      val fj = (y % scaleY) / scaleY.toDouble
      val fk = (z % scaleZ) / scaleZ.toDouble

      triLerp(
        data(ii, ij, ik),
        data(ii, ij, ik + 1),
        data(ii, ij + 1, ik),
        data(ii, ij + 1, ik + 1),
        data(ii + 1, ij, ik),
        data(ii + 1, ij, ik + 1),
        data(ii + 1, ij + 1, ik),
        data(ii + 1, ij + 1, ik + 1),
        fk,
        fj,
        fi
      )
    }
  }
}
