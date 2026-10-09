package hexacraft.client.entity

import hexacraft.world.entity.{EntityModel, EntityPart, HexPrism}

import munit.FunSuite
import org.joml.{Matrix4f, Vector3f}

class EntityPoseTest extends FunSuite {
  private val pi = math.Pi.toFloat
  private val px = EntityModel.pixelSize.toFloat

  private def assertMatrixEquals(actual: Matrix4f, expected: Matrix4f): Unit = {
    assert(actual.equals(expected, 1e-5f), s"expected:\n$expected\nbut was:\n$actual")
  }

  test("a part without a pose rotation gets its resting transform") {
    val part = EntityPart("leg", HexPrism(4, 32), Vector3f(64, 128, 192), Vector3f(pi, 0, 0))
    val pose = EntityPose(EntityModel(IndexedSeq(part)))

    val expected = Matrix4f().translate(1, 2, 3).rotateX(pi)
    assertMatrixEquals(pose.partTransforms(0), expected)
  }

  test("the pose rotation is applied before the resting rotation") {
    val part = EntityPart("leg", HexPrism(4, 32), Vector3f(), Vector3f(pi, 0, 0))
    val pose = EntityPose(EntityModel(IndexedSeq(part)))
    pose.rotation("leg").x = 0.1f
    pose.rotation("leg").y = 0.2f
    pose.rotation("leg").z = 0.3f

    val expected = Matrix4f().rotateZ(0.3f).rotateX(0.1f).rotateY(0.2f).rotateX(pi)
    assertMatrixEquals(pose.partTransforms(0), expected)
  }

  test("children are placed relative to their posed parent") {
    val parent = EntityPart("pivot", HexPrism.empty, Vector3f(0, 64, 0), Vector3f())
    val child = EntityPart("head", HexPrism(8, 16), Vector3f(0, 0, 32), Vector3f(), Some("pivot"))
    val pose = EntityPose(EntityModel(IndexedSeq(parent, child)))
    pose.rotation("pivot").y = pi / 2

    val expected = Matrix4f().translate(0, 1, 0).rotateY(pi / 2).translate(0, 0, 0.5f)
    assertMatrixEquals(pose.partTransforms(1), expected)
  }

  test("the prism is moved along the part's y-axis by the prism offset and scaled to the size of the prism") {
    val part = EntityPart("arm", HexPrism(8, 40), Vector3f(0, 64, 0), Vector3f(pi, 0, 0), prismOffset = -4)
    val pose = EntityPose(EntityModel(IndexedSeq(part)))

    val expected = Matrix4f().translate(0, 1, 0).rotateX(pi).translate(0, -4 * px, 0).scale(8 * px, 40 * px, 8 * px)
    assertMatrixEquals(pose.prismTransforms(0), expected)
  }

  test("the prism offset of a parent does not affect its children") {
    val parent = EntityPart("body", HexPrism(8, 40), Vector3f(), Vector3f(), prismOffset = -20)
    val child = EntityPart("head", HexPrism(8, 16), Vector3f(0, 32, 0), Vector3f(), Some("body"))
    val pose = EntityPose(EntityModel(IndexedSeq(parent, child)))

    val expected = Matrix4f().translate(0, 0.5f, 0)
    assertMatrixEquals(pose.partTransforms(1), expected)
  }

  test("the model can be shared since each pose has its own rotations") {
    val model = EntityModel(IndexedSeq(EntityPart("leg", HexPrism(4, 32), Vector3f(), Vector3f())))
    val pose1 = EntityPose(model)
    val pose2 = EntityPose(model)

    pose1.rotation("leg").z = 0.3f

    assertEquals(pose2.rotation("leg").z, 0f)
    assertEquals(model.parts(0).rotation.z(), 0f)
  }

  test("the rotation of a part that is not in the model is ignored, but the name is recorded") {
    val model = EntityModel(IndexedSeq(EntityPart("leg", HexPrism(4, 32), Vector3f(), Vector3f())))
    val pose = EntityPose(model)

    pose.rotation("tail").z = 0.3f

    assertEquals(pose.rotation("leg").z, 0f)
    assertMatrixEquals(pose.partTransforms(0), Matrix4f())
    assertEquals(pose.missingPartNames, Set("tail"))
  }

  test("a model requires parents to come before their children") {
    val parent = EntityPart("pivot", HexPrism.empty, Vector3f(), Vector3f())
    val child = EntityPart("head", HexPrism(8, 16), Vector3f(), Vector3f(), Some("pivot"))

    intercept[IllegalArgumentException](EntityModel(IndexedSeq(child, parent)))
  }

  test("a model requires the parent of each part to be in the model") {
    val child = EntityPart("head", HexPrism(8, 16), Vector3f(), Vector3f(), Some("pivot"))

    intercept[IllegalArgumentException](EntityModel(IndexedSeq(child)))
  }
}
