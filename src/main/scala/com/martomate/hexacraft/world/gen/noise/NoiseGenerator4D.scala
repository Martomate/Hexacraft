package com.martomate.hexacraft.world.gen.noise

import java.util.Random
import com.martomate.hexacraft.world.storage.World
import com.martomate.hexacraft.world.coord.CylCoords

class NoiseGenerator4D(random: Random, val numOctaves: Int, val scale: Double) {
  private[this] val noiseGens = Seq.fill(numOctaves)(new SingleNoiseGen4D(random))

  def genNoise(x: Double, y: Double, z: Double, w: Double): Double = {
    var amp = 1d
    var result = 0d
    for (n <- noiseGens) {
      val mult = scale / amp
      result += amp * n.noise(x * mult, y * mult, z * mult, w * mult)
      amp /= 2
    }
    result
  }

  def genNoiseFromCyl(c: CylCoords): Double = {
    val angle = c.z / c.cylSize.radius
    genNoise(c.x, c.y, math.sin(angle) * c.cylSize.radius, math.cos(angle) * c.cylSize.radius)
  }
}
