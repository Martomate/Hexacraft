package hexacraft.server.world

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

  /** A camera at the given position. The direction of the camera doesn't matter, since the ray is given separately. */
  private def cameraAt(x: Double, y: Double, z: Double): Camera = {
    val camera = new Camera(new CameraProjection(70f, 1f, 0.02f, 1000f))
    camera.setPositionAndRotation(new Vector3d(x, y, z), new Vector3d)
    camera
  }

  private val down = Ray(new Vector3d(0, -1, 0))

  /** The distance from the camera to the point, measured the way picking does it (taking the curvature of the world
    * into account). The faces are planes through their corners, while the surface of the curved world bends slightly,
    * so the distance to a face can differ from this by a tiny amount (less than 1e-3 here).
    */
  private def distanceTo(camera: Camera, point: CylCoords): Double = {
    point.toNormalCoords(CylCoords(camera.position)).toVector3d.length
  }

  /** A model with a single rod (radius 4 and length 128 pixels, i.e. 2 units) lying along the positive x-axis */
  private val rod = PlacedModel(
    EntityModel(IndexedSeq(EntityPart("rod", HexPrism(4, 128), Vector3f(), Vector3f(0, 0, -pi / 2))))
  )

  test("a ray from above hits the far end of a long part") {
    // The rod reaches x = 2, which is far outside a small bounding box around the entity's position
    val camera = cameraAt(1.9, 3, 0)
    val distance = EntityPicking.distanceToModel(rod, CylCoords(0, 0, 0), new Vector3d, camera, down)

    // The top of the rod is at a corner of the hexagon, 4 pixels above the axis
    assertEqualsDouble(distance.get, distanceTo(camera, CylCoords(1.9, 4 * px, 0)), 1e-3)
  }

  test("a ray that passes beside a part misses it") {
    assertEquals(EntityPicking.distanceToModel(rod, CylCoords(0, 0, 0), new Vector3d, cameraAt(2.1, 3, 0), down), None)
    assertEquals(EntityPicking.distanceToModel(rod, CylCoords(0, 0, 0), new Vector3d, cameraAt(1, 3, 0.2), down), None)
  }

  test("the parts follow the rotation of the entity around the y-axis") {
    val rotation = new Vector3d(0, math.Pi / 2, 0) // the rod now lies along the z-axis

    val alongX = EntityPicking.distanceToModel(rod, CylCoords(0, 0, 0), rotation, cameraAt(1.9, 3, 0), down)
    val alongZ1 = EntityPicking.distanceToModel(rod, CylCoords(0, 0, 0), rotation, cameraAt(0, 3, 1.9), down)
    val alongZ2 = EntityPicking.distanceToModel(rod, CylCoords(0, 0, 0), rotation, cameraAt(0, 3, -1.9), down)

    assertEquals(alongX, None)
    assert(alongZ1.isDefined || alongZ2.isDefined, "the rod should be somewhere along the z-axis")
  }

  test("the parts follow the tilt of the entity around the x-axis") {
    // An upright pillar (2 units tall) that is tilted so that it lies along the z-axis
    val pillar = PlacedModel(EntityModel(IndexedSeq(EntityPart("pillar", HexPrism(4, 128), Vector3f(), Vector3f()))))
    val rotation = new Vector3d(math.Pi / 2, 0, 0)

    val upright = EntityPicking.distanceToModel(pillar, CylCoords(0, 0, 0), new Vector3d, cameraAt(0, 3, 0), down)
    val tilted = EntityPicking.distanceToModel(pillar, CylCoords(0, 0, 0), rotation, cameraAt(0, 3, 0), down)
    val tiltedFarEnd = EntityPicking.distanceToModel(pillar, CylCoords(0, 0, 0), rotation, cameraAt(0, 3, 1.9), down)

    assertEqualsDouble(upright.get, distanceTo(cameraAt(0, 3, 0), CylCoords(0, 2, 0)), 1e-3) // the top is at y = 2
    assert(tilted.exists(_ > 2.5), tilted) // only the thin side of the lying pillar is below the camera
    assert(tiltedFarEnd.isDefined, "the far end of the tilted pillar should be at z = 2")
  }

  test("the closest part is hit") {
    val model = PlacedModel(
      EntityModel(
        IndexedSeq(
          EntityPart("low", HexPrism(8, 16), Vector3f(), Vector3f()),
          EntityPart("high", HexPrism(8, 16), Vector3f(0, 64, 0), Vector3f())
        )
      )
    )

    val camera = cameraAt(0, 3, 0)
    val distance = EntityPicking.distanceToModel(model, CylCoords(0, 0, 0), new Vector3d, camera, down)
    assertEqualsDouble(distance.get, distanceTo(camera, CylCoords(0, 80 * px, 0)), 1e-3) // the top of the high part
  }

  test("a ray through a gap between two parts misses the model") {
    val model = PlacedModel(
      EntityModel(
        IndexedSeq(
          EntityPart("left", HexPrism(8, 16), Vector3f(0, 0, -32), Vector3f()),
          EntityPart("right", HexPrism(8, 16), Vector3f(0, 0, 32), Vector3f())
        )
      )
    )

    assertEquals(EntityPicking.distanceToModel(model, CylCoords(0, 0, 0), new Vector3d, cameraAt(0, 3, 0), down), None)
  }

  test("the front of the boat can be clicked") {
    val boat = Entity.atStartPos(UUID.randomUUID(), CylCoords(0, 0, 0), "boat").unwrap()
    val placedModel = PlacedModel.forType("boat").get

    // The point of the boat's model that is furthest from the entity's position (horizontally)
    val farthest = placedModel.prismCorners
      .flatMap(c => c.top ++ c.bottom)
      .maxBy(c => math.hypot(c.x, c.z))
    assert(math.hypot(farthest.x, farthest.z) > boat.boundingBox.radius + 0.5, "the boat is longer than its bounds")

    // Aim a bit inside that point, from above
    val target = new Vector3d(farthest.x, 0, farthest.z).mul(0.95)
    val camera = cameraAt(target.x, 3, target.z)

    assert(EntityPicking.distanceToEntity(boat, camera, down).isDefined)
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
