package hexacraft.client.entity

import hexacraft.world.{CylinderSize, HexBox}
import hexacraft.world.coord.CylCoords

/** Helpers for building entity models. Lengths are given in model pixels (1/32 of a block). */
private[entity] object ModelUnits {

  /** Converts a length in model pixels to cylinder coordinates. */
  def px(n: Double): Double = n / 32 * 0.5

  def cylOffset(x: Double, y: Double, z: Double): CylCoords.Offset = CylCoords.Offset(px(x), px(y), px(z))

  /** The distance between the centers of two neighbouring hexagons of the given radius (in a row). */
  def hexStep(radius: Double): Double = 2 * radius * CylinderSize.y60

  /** A hexagonal prism with a bottom at `b` and a height of `h` above that, in model pixels. */
  def makeHexBox(radius: Double, b: Double, h: Double): HexBox =
    HexBox(px(radius).toFloat, px(b).toFloat, px(h + b).toFloat)
}
