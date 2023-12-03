package hexacraft.physics

opaque type Density <: AnyVal = Double

object Density {

  /** Unit: `kg/m^3` */
  def fromSI(d: Double): Density = d

  val water: Density = fromSI(1000)

  extension (d: Density) {

    /** Unit: `kg/m^3` */
    def toSI: Double = d
  }
}
