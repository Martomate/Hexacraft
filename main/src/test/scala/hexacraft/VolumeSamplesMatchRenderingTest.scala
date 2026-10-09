package hexacraft

import hexacraft.client.entity.EntityPose
import hexacraft.server.entity.EntityModels
import hexacraft.server.world.VolumeSamples

import munit.FunSuite
import org.joml.{Matrix4f, Vector3f}

/** The server samples the volume of the models for its physics, while the client renders them. This test makes sure
  * that they place the parts in the same way.
  */
class VolumeSamplesMatchRenderingTest extends FunSuite {

  /** Is the point inside the unit prism (a hexagon with radius 1 and corners at multiples of 60 degrees, from y = 0 to
    * y = 1), which is what the client renders for each part?
    */
  private def isInsideUnitPrism(p: Vector3f): Boolean = {
    val eps = 1e-3
    val insideHexagon = (0 until 6).forall { side =>
      val angle = (side + 0.5) * scala.math.Pi / 3
      p.x * scala.math.cos(angle) + p.z * scala.math.sin(angle) <= scala.math.sqrt(3) / 2 + eps
    }
    insideHexagon && p.y >= -eps && p.y <= 1 + eps
  }

  for entityType <- Seq("player", "sheep", "boat") do {
    test(s"every volume sample of '$entityType' is inside one of the prisms that the client renders") {
      val model = EntityModels.forType(entityType).get
      val samples = VolumeSamples.fromModel(model)

      val pose = EntityPose(model)
      val inversePrismTransforms = model.parts.indices
        .filter(idx => model.parts(idx).isVisible)
        .map(idx => Matrix4f(pose.prismTransforms(idx)).invert())

      for s <- samples.samples do {
        val point = Vector3f(s.offset.x.toFloat, s.offset.y.toFloat, s.offset.z.toFloat)
        val inside = inversePrismTransforms.exists(t => isInsideUnitPrism(t.transformPosition(point, Vector3f())))
        assert(inside, s"the sample at ${s.offset} is not inside any of the rendered prisms")
      }
    }
  }
}
