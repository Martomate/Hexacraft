package com.martomate.hexacraft.world.ray

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.{BlocksInWorld, FakeBlockLoader, FakeBlocksInWorld, FakeWorldProvider}
import com.martomate.hexacraft.world.block.{BlockFactory, BlockLoader, Blocks, BlockState}
import com.martomate.hexacraft.world.camera.{Camera, CameraProjection}
import com.martomate.hexacraft.world.coord.fp.BlockCoords
import com.martomate.hexacraft.world.coord.integer.BlockRelWorld
import com.martomate.hexacraft.world.entity.EntityModelLoader
import com.martomate.hexacraft.world.ray.{Ray, RayTracer}

import munit.FunSuite
import org.joml.Vector2f

class RayTracerTest extends FunSuite {
  given CylinderSize = CylinderSize(8)
  given BlockLoader = new FakeBlockLoader
  given BlockFactory = new BlockFactory
  given Blocks: Blocks = new Blocks
  given EntityModelLoader = new EntityModelLoader

  private def makeCameraProjection = new CameraProjection(70, 1.6f, 0.01f, 1000f)

  test("the raytracer should not crash") {
    val world: BlocksInWorld = FakeBlocksInWorld.empty(new FakeWorldProvider(1289))
    val camera = new Camera(makeCameraProjection)

    val ray = Ray.fromScreen(camera, new Vector2f(-0.3f, 0.2f)).get

    // There are no blocks so it should fail
    val rayTracer = new RayTracer(world, camera, 5.0)
    assertEquals(rayTracer.trace(ray, _ => true), None)
  }

  test("the raytracer should return a block if the camera is in it") {
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
    assertEquals(rayTracer.trace(ray, _ => true), Some((location, None)))
  }

  test("the raytracer should return None if the mouse is not on the screen") {
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
    assertEquals(rayTracer.trace(ray, _ => true), Some((BlockRelWorld(0, 0, 1), Some(6))))
  }

  // TODO: there is a bug where a ray can escape confinement if it starts at (0,0,0) with a y-rotation of Pi/2
}
