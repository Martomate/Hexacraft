package hexacraft.renderer

import org.joml.Vector3f

case class PixelArray(pixels: Array[Int], isTriImage: Boolean) {
  def averageColor: Vector3f = {
    var r = 0L
    var g = 0L
    var b = 0L

    for pix <- this.pixels do {
      r += (pix >> 16) & 0xff
      g += (pix >> 8) & 0xff
      b += (pix >> 0) & 0xff
    }

    val c = this.pixels.length * 255
    Vector3f(r.toFloat / c, g.toFloat / c, b.toFloat / c)
  }
}
