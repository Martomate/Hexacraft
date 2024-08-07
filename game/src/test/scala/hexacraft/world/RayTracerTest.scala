package hexacraft.world

import hexacraft.world.*
import hexacraft.world.block.{Block, BlockState}
import hexacraft.world.coord.{BlockCoords, BlockRelWorld}
import munit.FunSuite
import org.joml.Vector2f

class RayTracerTest extends FunSuite {
  given CylinderSize = CylinderSize(8)

  private def makeCameraProjection = new CameraProjection(70, 1.6f, 0.01f, 1000f)

  test("the raytracer should not crash") {
    val camera = new Camera(makeCameraProjection)

    val ray = Ray.fromScreen(camera, new Vector2f(-0.3f, 0.2f)).get

    // There are no blocks so it should fail
    val rayTracer = new RayTracer(camera, 5.0)
    assertEquals(rayTracer.trace(ray, _ => None), None)
  }

  test("the raytracer should return a block if the camera is in it") {
    val provider = new FakeWorldProvider(1289)
    val location = BlockRelWorld(-37, 3, 1)
    val world: BlocksInWorld = FakeBlocksInWorld.withBlocks(
      provider,
      Map(
        location -> new BlockState(Block.Dirt)
      )
    )

    // Create a camera at the location
    val camera = new Camera(makeCameraProjection)
    camera.position.set(BlockCoords(location).offset(0, 0.5, 0).toCylCoords.toVector3d)
    camera.updateCoords()
    camera.updateViewMatrix(camera.view.position)

    // Create a ray
    val ray = Ray.fromScreen(camera, new Vector2f(0, 0)).get

    // The ray tracer should find the block
    val rayTracer = new RayTracer(camera, 5.0)
    assertEquals(rayTracer.trace(ray, coords => Some(world.getBlock(coords))), Some((location, None)))
  }

  test("the raytracer should return None if the mouse is not on the screen") {
    val provider = new FakeWorldProvider(1289)
    val location = BlockRelWorld(-37, 3, 1)
    val world: BlocksInWorld = FakeBlocksInWorld.withBlocks(
      provider,
      Map(
        location -> new BlockState(Block.Dirt)
      )
    )

    // Create a camera at the location
    val camera = new Camera(makeCameraProjection)
    camera.position.set(BlockCoords(location).offset(0, 0.5, 0).toCylCoords.toVector3d)
    camera.updateCoords()
    camera.updateViewMatrix(camera.view.position)

    // Look outside of the screen
    assertEquals(Ray.fromScreen(camera, new Vector2f(1.2f, 0)), None)
    assertEquals(Ray.fromScreen(camera, new Vector2f(-1.2f, 0)), None)
    assertEquals(Ray.fromScreen(camera, new Vector2f(0, 1.2f)), None)
    assertEquals(Ray.fromScreen(camera, new Vector2f(0, -1.2f)), None)
    assertEquals(Ray.fromScreen(camera, new Vector2f(1.3f, -1.2f)), None)
  }

  test("the raytracer should return a block right in front of the camera") {
    val provider = new FakeWorldProvider(1289)
    val world: BlocksInWorld = FakeBlocksInWorld.withBlocks(
      provider,
      Map(
        BlockRelWorld(0, 0, 0) -> new BlockState(Block.Air),
        BlockRelWorld(0, 0, 1) -> new BlockState(Block.Dirt)
      )
    )

    // Create a camera looking in the positive Z-direction
    val camera = new Camera(makeCameraProjection)
    camera.position.set(BlockCoords(0, 0.5, 0).toCylCoords.toVector3d)
    camera.rotation.set(0, math.Pi.toFloat, 0)
    camera.updateCoords()
    camera.updateViewMatrix(camera.view.position)

    // Create a ray
    val ray = Ray.fromScreen(camera, new Vector2f(0, 0)).get

    // The ray tracer should find the block
    val rayTracer = new RayTracer(camera, 5.0)
    assertEquals(rayTracer.trace(ray, coords => Some(world.getBlock(coords))), Some((BlockRelWorld(0, 0, 1), Some(6))))
  }

  // TODO: there is a bug where a ray can escape confinement if it starts at (0,0,0) with a y-rotation of Pi/2
}
