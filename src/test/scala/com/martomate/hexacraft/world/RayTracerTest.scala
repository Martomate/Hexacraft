package com.martomate.hexacraft.world

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.Blocks
import com.martomate.hexacraft.world.block.state.BlockState
import com.martomate.hexacraft.world.camera.{Camera, CameraProjection}
import com.martomate.hexacraft.world.coord.fp.BlockCoords
import com.martomate.hexacraft.world.coord.integer.BlockRelWorld
import org.joml.Vector2f
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class RayTracerTest extends AnyFlatSpec with Matchers {
  implicit val cylSize: CylinderSize = new CylinderSize(8)

  private def makeCameraProjection = new CameraProjection(70, 1.6f, 0.01f, 1000f)

  "the raytracer" should "not crash" in {
    val world: BlocksInWorld = FakeBlocksInWorld.empty(new FakeWorldProvider(1289))
    val camera = new Camera(makeCameraProjection)

    val ray = Ray.fromScreen(camera, new Vector2f(-0.3f, 0.2f)).get

    // There are no blocks so it should fail
    val rayTracer = new RayTracer(world, camera, 5.0)
    rayTracer.trace(ray, _ => true) shouldBe None
  }

  it should "return a block if the camera is in it" in {
    val provider = new FakeWorldProvider(1289)
    val location = BlockRelWorld(-37, 3, 1)
    val world: BlocksInWorld = FakeBlocksInWorld.withBlocks(
      provider,
      Map(
        location -> new BlockState(Blocks.Dirt)
      )
    )

    // Create a camera at the location
    val camera = new Camera(makeCameraProjection)
    camera.position.set(BlockCoords(location).offset(0, 0.5, 0).toCylCoords.toVector3d)
    camera.updateCoords()
    camera.updateViewMatrix()

    // Create a ray
    val ray = Ray.fromScreen(camera, new Vector2f(0, 0)).get

    // The ray tracer should find the block
    val rayTracer = new RayTracer(world, camera, 5.0)
    rayTracer.trace(ray, _ => true) shouldBe Some((location, None))
  }

  it should "return None if the mouse is not on the screen" in {
    val provider = new FakeWorldProvider(1289)
    val location = BlockRelWorld(-37, 3, 1)
    val world: BlocksInWorld = FakeBlocksInWorld.withBlocks(
      provider,
      Map(
        location -> new BlockState(Blocks.Dirt)
      )
    )

    // Create a camera at the location
    val camera = new Camera(makeCameraProjection)
    camera.position.set(BlockCoords(location).offset(0, 0.5, 0).toCylCoords.toVector3d)
    camera.updateCoords()
    camera.updateViewMatrix()

    // Look outside of the screen
    Ray.fromScreen(camera, new Vector2f(1.2f, 0)) shouldBe None
    Ray.fromScreen(camera, new Vector2f(-1.2f, 0)) shouldBe None
    Ray.fromScreen(camera, new Vector2f(0, 1.2f)) shouldBe None
    Ray.fromScreen(camera, new Vector2f(0, -1.2f)) shouldBe None
    Ray.fromScreen(camera, new Vector2f(1.3f, -1.2f)) shouldBe None
  }

  it should "return a block right in front of the camera" in {
    val provider = new FakeWorldProvider(1289)
    val world: BlocksInWorld = FakeBlocksInWorld.withBlocks(
      provider,
      Map(
        BlockRelWorld(0, 0, 0) -> new BlockState(Blocks.Air),
        BlockRelWorld(0, 0, 1) -> new BlockState(Blocks.Dirt)
      )
    )

    // Create a camera looking in the positive Z-direction
    val camera = new Camera(makeCameraProjection)
    camera.position.set(BlockCoords(0, 0.5, 0).toCylCoords.toVector3d)
    camera.rotation.set(0, math.Pi.toFloat, 0)
    camera.updateCoords()
    camera.updateViewMatrix()

    // Create a ray
    val ray = Ray.fromScreen(camera, new Vector2f(0, 0)).get

    // The ray tracer should find the block
    val rayTracer = new RayTracer(world, camera, 5.0)
    rayTracer.trace(ray, _ => true) shouldBe Some((BlockRelWorld(0, 0, 1), Some(6)))
  }

  // TODO: there is a bug where a ray can escape confinement if it starts at (0,0,0) with a y-rotation of Pi/2
}
