package hexacraft

import hexacraft.client.entity.EntityPose
import hexacraft.server.entity.EntityModels
import hexacraft.server.world.PlacedModel

import munit.FunSuite
import org.joml.Vector3f

/** The server uses the corners of the prisms of the models for picking, while the client renders them. This test makes
  * sure that the server's corners are the corners of the prisms that the client renders.
  */
class PlacedModelMatchesRenderingTest extends FunSuite {
  for entityType <- Seq("player", "sheep", "boat") do {
    test(s"the prism corners of '$entityType' on the server are the corners of the prisms rendered by the client") {
      val model = EntityModels.forType(entityType).get
      val placedModel = PlacedModel(model)
      val pose = EntityPose(model)

      val visiblePartIndices = model.parts.indices.filter(idx => model.parts(idx).isVisible)
      assertEquals(placedModel.prismCorners.size, visiblePartIndices.size)

      for (corners, idx) <- placedModel.prismCorners.zip(visiblePartIndices) do {
        // The client renders a unit prism (radius 1, from y = 0 to y = 1) transformed by the prism transform
        val prismTransform = pose.prismTransforms(idx)

        for i <- 0 until 6 do {
          val angle = i * scala.math.Pi / 3
          val x = scala.math.cos(angle).toFloat
          val z = scala.math.sin(angle).toFloat

          val renderedTop = prismTransform.transformPosition(Vector3f(x, 1, z), Vector3f())
          val renderedBottom = prismTransform.transformPosition(Vector3f(x, 0, z), Vector3f())

          val top = corners.top(i)
          val bottom = corners.bottom(i)
          assert(
            renderedTop.distance(top.x.toFloat, top.y.toFloat, top.z.toFloat) < 1e-5,
            s"top corner $i of part $idx"
          )
          assert(
            renderedBottom.distance(bottom.x.toFloat, bottom.y.toFloat, bottom.z.toFloat) < 1e-5,
            s"bottom corner $i of part $idx"
          )
        }
      }
    }
  }
}
