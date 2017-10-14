package hexagon.world.gen.noise

import java.util.Random
import hexagon.world.storage.World
import hexagon.world.coord.CylCoord

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

  def genNoiseFromCyl(c: CylCoord): Double = {
    val angle = c.z / World.radius
    genNoise(c.x, c.y, math.sin(angle) * World.radius, math.cos(angle) * World.radius)
  }
}
