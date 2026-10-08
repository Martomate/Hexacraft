package hexacraft.client.entity

import hexacraft.world.CylinderSize

import org.joml.Vector3f

/** Helpers for building entity models. Lengths are given in model pixels (1/32 of a block). */
private[entity] object ModelUnits {

  def pos(x: Double, y: Double, z: Double): Vector3f = Vector3f(x.toFloat, y.toFloat, z.toFloat)

  /** The distance between the centers of two neighbouring hexagons of the given radius (in a row). */
  def hexStep(radius: Double): Double = 2 * radius * CylinderSize.y60
}
