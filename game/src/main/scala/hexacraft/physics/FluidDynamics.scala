package hexacraft.physics

import org.joml.{Vector3d, Vector3dc}

object FluidDynamics {

  /** @param area usually the orthographic projection of the object into the direction of movement
    * @return the fluid drag (calculated using the drag equation)
    */
  def dragForce(
      velocity: Vector3dc,
      dragCoefficient: DragCoefficient,
      area: Double,
      fluidDensity: Density
  ): Vector3d = {
    val rho = fluidDensity.toSI
    val Cd = dragCoefficient.toDouble
    val A = area

    // (0.5 * rho * v^2 * Cd * A) in the opposite direction of velocity
    velocity.mul(-0.5 * rho * velocity.length() * Cd * A, new Vector3d)
  }
}
