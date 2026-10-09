package hexacraft.client

import hexacraft.client.entity.{EntityAnimation, EntityPose, EntitySkins, ModelComponent}
import hexacraft.world.{Camera, CameraProjection, CylinderSize, HexBox, Ray}
import hexacraft.world.coord.CylCoords
import hexacraft.world.entity.*

import munit.FunSuite
import org.joml.{Vector3d, Vector3f}

import java.util.UUID

class EntityPickingTest extends FunSuite {
  given CylinderSize = CylinderSize(8)

  private val px = EntityModel.pixelSize
  private val pi = math.Pi.toFloat

  private val down = Ray(new Vector3d(0, -1, 0))

  private def cameraAt(x: Double, y: Double, z: Double): Camera = {
    val camera = new Camera(new CameraProjection(70f, 1f, 0.02f, 1000f))
    camera.setPositionAndRotation(new Vector3d(x, y, z), new Vector3d)
    camera
  }

  /** The distance from the camera to the point, measured the way picking does it (taking the curvature of the world
    * into account). The faces are planes through their corners, while the surface of the curved world bends slightly,
    * so the distance to a face can differ from this by a tiny amount (less than 1e-3 here).
    */
  private def distanceTo(camera: Camera, point: CylCoords): Double = {
    point.toNormalCoords(CylCoords(camera.position)).toVector3d.length
  }

  /** A model with a pivot and an arm (2 units long) pointing along the pivot's y-axis */
  private val armModel = EntityModel(
    IndexedSeq(
      EntityPart("pivot", HexPrism.empty, Vector3f(), Vector3f(0, 0, -pi / 2)), // the arm points along +x
      EntityPart("arm", HexPrism(4, 128), Vector3f(), Vector3f(), Some("pivot"))
    )
  )

  private def entityWithModel(position: CylCoords, model: EntityModel = armModel): (Entity, EntityPose) = {
    val pose = EntityPose(model)
    val entity = Entity
      .atStartPos(UUID.randomUUID(), position, "sheep")
      .unwrap()
      .withComponent(ModelComponent(pose, EntitySkins.sheep, EntityAnimation.none))
    (entity, pose)
  }

  test("a ray hits a part of the entity's model") {
    val (entity, _) = entityWithModel(CylCoords(0, 0, 0))
    val camera = cameraAt(1.9, 3, 0)

    val distance = EntityPicking.distanceToEntity(entity, camera, down)
    assertEqualsDouble(distance.get, distanceTo(camera, CylCoords(1.9, 4 * px, 0)), 1e-3)
  }

  test("a ray that passes beside the model misses it") {
    val (entity, _) = entityWithModel(CylCoords(0, 0, 0))

    assertEquals(EntityPicking.distanceToEntity(entity, cameraAt(2.1, 3, 0), down), None)
    assertEquals(EntityPicking.distanceToEntity(entity, cameraAt(1, 3, 0.2), down), None)
  }

  test("the parts are tested in their current (animated) pose") {
    val (entity, pose) = entityWithModel(CylCoords(0, 0, 0))

    pose.rotation("pivot").y = pi / 2 // swing the arm around so that it points along the z-axis

    assertEquals(EntityPicking.distanceToEntity(entity, cameraAt(1.9, 3, 0), down), None)
    val alongZ = Seq(1.9, -1.9).flatMap(z => EntityPicking.distanceToEntity(entity, cameraAt(0, 3, z), down))
    assert(alongZ.nonEmpty, "the arm should be somewhere along the z-axis")
  }

  test("the parts follow the rotation of the entity") {
    val (entity, _) = entityWithModel(CylCoords(0, 0, 0))
    entity.transform.rotation.set(0, math.Pi / 2, 0)

    assertEquals(EntityPicking.distanceToEntity(entity, cameraAt(1.9, 3, 0), down), None)
    val alongZ = Seq(1.9, -1.9).flatMap(z => EntityPicking.distanceToEntity(entity, cameraAt(0, 3, z), down))
    assert(alongZ.nonEmpty, "the arm should be somewhere along the z-axis")
  }

  test("the closest entity is the one that counts") {
    val (low, _) = entityWithModel(CylCoords(0, 0, 0))
    val (high, _) = entityWithModel(CylCoords(0, 1, 0))
    val camera = cameraAt(1, 3, 0)

    val distance = EntityPicking.closestEntityDistance(Seq(low, high), camera, down, 10)
    assertEqualsDouble(distance.get, distanceTo(camera, CylCoords(1, 1 + 4 * px, 0)), 1e-3)
  }

  test("entities further away than the max distance are ignored") {
    val (entity, _) = entityWithModel(CylCoords(0, 0, 0))
    val camera = cameraAt(1, 3, 0)

    assert(EntityPicking.closestEntityDistance(Seq(entity), camera, down, 3).isDefined)
    assertEquals(EntityPicking.closestEntityDistance(Seq(entity), camera, down, 2.5), None)
    assertEquals(EntityPicking.closestEntityDistance(Seq(), camera, down, 3), None)
  }

  test("entities without a model are picked using their bounding box") {
    val components = Seq(TransformComponent(CylCoords(0, 0, 0)), MotionComponent(), BoundsComponent(HexBox(0.5f, 0, 1)))
    val unicorn = Entity(UUID.randomUUID(), "unicorn", components)
    val camera = cameraAt(0, 3, 0)

    assertEqualsDouble(
      EntityPicking.distanceToEntity(unicorn, camera, down).get,
      distanceTo(camera, CylCoords(0, 1, 0)),
      1e-3
    )
    assertEquals(EntityPicking.distanceToEntity(unicorn, cameraAt(1, 3, 0), down), None)
  }
}
