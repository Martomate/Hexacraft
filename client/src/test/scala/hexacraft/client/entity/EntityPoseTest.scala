package hexacraft.client.entity

import hexacraft.world.HexBox
import hexacraft.world.coord.CylCoords
import hexacraft.world.entity.{EntityModel, EntityPart}

import munit.FunSuite
import org.joml.{Matrix4f, Vector3f}

class EntityPoseTest extends FunSuite {
  private val pi = math.Pi.toFloat

  private def assertMatrixEquals(actual: Matrix4f, expected: Matrix4f): Unit = {
    assert(actual.equals(expected, 1e-5f), s"expected:\n$expected\nbut was:\n$actual")
  }

  test("a part without a pose rotation gets its resting transform") {
    val part = EntityPart("leg", HexBox(0.1f, 0.2f, 0.5f), CylCoords.Offset(1, 2, 3), Vector3f(pi, 0, 0))
    val pose = EntityPose(EntityModel(IndexedSeq(part)))

    val expected = Matrix4f().translate(1, 2, 3).rotateX(pi).translate(0, 0.2f, 0)
    assertMatrixEquals(pose.partTransforms(0), expected)
  }

  test("the pose rotation is applied after the resting rotation") {
    val part = EntityPart("leg", HexBox(0.1f, 0, 0.5f), CylCoords.Offset(0, 0, 0), Vector3f(pi, 0, 0))
    val pose = EntityPose(EntityModel(IndexedSeq(part)))
    pose.rotation("leg").z = 0.3f

    val expected = Matrix4f().rotateZ(0.3f).rotateX(pi)
    assertMatrixEquals(pose.partTransforms(0), expected)
  }

  test("children are placed relative to their posed parent") {
    val parent = EntityPart("pivot", HexBox(0, 0, 0), CylCoords.Offset(0, 1, 0), Vector3f())
    val child = EntityPart("head", HexBox(0.1f, 0, 0.2f), CylCoords.Offset(0, 0, 0.5), Vector3f(), Some("pivot"))
    val pose = EntityPose(EntityModel(IndexedSeq(parent, child)))
    pose.rotation("pivot").y = pi / 2

    val expected = Matrix4f().translate(0, 1, 0).rotateY(pi / 2).translate(0, 0, 0.5f)
    assertMatrixEquals(pose.partTransforms(1), expected)
  }

  test("the model can be shared since each pose has its own rotations") {
    val model = EntityModel(IndexedSeq(EntityPart("leg", HexBox(0.1f, 0, 0.5f), CylCoords.Offset(0, 0, 0), Vector3f())))
    val pose1 = EntityPose(model)
    val pose2 = EntityPose(model)

    pose1.rotation("leg").z = 0.3f

    assertEquals(pose2.rotation("leg").z, 0f)
    assertEquals(model.parts(0).rotation.z(), 0f)
  }

  test("a model requires parents to come before their children") {
    val parent = EntityPart("pivot", HexBox(0, 0, 0), CylCoords.Offset(0, 0, 0), Vector3f())
    val child = EntityPart("head", HexBox(0.1f, 0, 0.2f), CylCoords.Offset(0, 0, 0), Vector3f(), Some("pivot"))

    intercept[IllegalArgumentException](EntityModel(IndexedSeq(child, parent)))
  }

  test("a model requires the parent of each part to be in the model") {
    val child = EntityPart("head", HexBox(0.1f, 0, 0.2f), CylCoords.Offset(0, 0, 0), Vector3f(), Some("pivot"))

    intercept[IllegalArgumentException](EntityModel(IndexedSeq(child)))
  }
}
