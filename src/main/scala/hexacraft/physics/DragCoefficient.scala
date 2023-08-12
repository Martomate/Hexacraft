package hexacraft.physics

/** How much drag (e.g. air resistance) an object of a particular shape will experience in a fluid.
  *
  * The value may depend (slightly) on Reynolds number (which depends on the object speed and some fluid parameters)
  */
opaque type DragCoefficient <: AnyVal = Double

object DragCoefficient {
  def fromDouble(d: Double): DragCoefficient = d

  /** Cd = 1 is a good approximation for humans */
  val human: DragCoefficient = 1

  extension (d: DragCoefficient) {
    def toDouble: Double = d
  }
}
