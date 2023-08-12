package hexacraft.physics

opaque type Speed <: AnyVal = Double

object Speed {

  /** Unit: `m/s` */
  def fromSI(s: Double): Speed = s

  extension (s: Speed) {
    def toSI: Double = s
  }
}
