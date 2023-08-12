package hexacraft.physics

opaque type Viscosity <: AnyVal = Double

object Viscosity {

  /** @param v viscosity in `Pa s` */
  def fromSI(v: Double): Viscosity = v

  val water: Viscosity = 1e-3
  val air: Viscosity = 18.5e-6

  extension (v: Viscosity) {
    def toSI: Double = v
  }
}
