package hexacraft.physics

import munit.FunSuite
import org.joml.Vector3d

class FluidDynamicsTest extends FunSuite {
  test("drag force is 0 when velocity is 0") {
    assertEqualsDouble(
      FluidDynamics
        .dragForce(new Vector3d(0), DragCoefficient.fromDouble(1), 1, Density.fromSI(1))
        .lengthSquared(),
      0,
      1e-9
    )
  }
}
