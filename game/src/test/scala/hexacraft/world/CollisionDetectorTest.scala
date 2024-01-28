package hexacraft.world

import hexacraft.world.block.*
import hexacraft.world.chunk.Chunk
import hexacraft.world.coord.{BlockCoords, BlockRelWorld, CylCoords, SkewCylCoords}

import munit.FunSuite
import org.joml.Vector3d

class CollisionDetectorTest extends FunSuite {
  given CylinderSize = CylinderSize(8)

  private val box1 = new HexBox(0.4f, 0.1f, 0.3f)
  private val box2 = new HexBox(0.5f, 0.15f, 0.45f)
  private val pos: CylCoords = BlockCoords(1, 27, -3).toCylCoords

  test("collides should return true for boxes at the same location") {
    val provider = new FakeWorldProvider(37)
    val world = FakeBlocksInWorld.empty(provider)
    val detector = new CollisionDetector(world)

    assert(detector.collides(box1, pos, box2, pos))
  }

  test("collides should work in the y-direction") {
    val provider = new FakeWorldProvider(37)
    val world = FakeBlocksInWorld.empty(provider)
    val detector = new CollisionDetector(world)

    val pos2a = pos.toSkewCylCoords.offset(0, box1.top - box2.bottom - 0.001f, 0).toCylCoords
    val pos2b = pos.toSkewCylCoords.offset(0, box1.top - box2.bottom + 0.001f, 0).toCylCoords
    val pos2c = pos.toSkewCylCoords.offset(0, box1.bottom - box2.top + 0.001f, 0).toCylCoords
    val pos2d = pos.toSkewCylCoords.offset(0, box1.bottom - box2.top - 0.001f, 0).toCylCoords

    assert(detector.collides(box1, pos, box2, pos2a))
    assert(!detector.collides(box1, pos, box2, pos2b))
    assert(detector.collides(box1, pos, box2, pos2c))
    assert(!detector.collides(box1, pos, box2, pos2d))
  }

  test("collides should work in the z-direction") {
    val provider = new FakeWorldProvider(37)
    val world = FakeBlocksInWorld.empty(provider)
    val detector = new CollisionDetector(world)

    val d = box1.smallRadius + box2.smallRadius

    val pos2a = pos.toSkewCylCoords.offset(0, 0, d - 0.001).toCylCoords
    val pos2b = pos.toSkewCylCoords.offset(0, 0, d + 0.001f).toCylCoords
    val pos2c = pos.toSkewCylCoords.offset(0, 0, -d + 0.001f).toCylCoords
    val pos2d = pos.toSkewCylCoords.offset(0, 0, -d - 0.001f).toCylCoords

    assert(detector.collides(box1, pos, box2, pos2a))
    assert(!detector.collides(box1, pos, box2, pos2b))
    assert(detector.collides(box1, pos, box2, pos2c))
    assert(!detector.collides(box1, pos, box2, pos2d))
  }

  test("collides should work in the x-direction") {
    val provider = new FakeWorldProvider(37)
    val world = FakeBlocksInWorld.empty(provider)
    val detector = new CollisionDetector(world)

    val d = box1.smallRadius + box2.smallRadius

    val pos2a = pos.toSkewCylCoords.offset(d - 0.001, 0, 0).toCylCoords
    val pos2b = pos.toSkewCylCoords.offset(d + 0.001f, 0, 0).toCylCoords
    val pos2c = pos.toSkewCylCoords.offset(-d + 0.001f, 0, 0).toCylCoords
    val pos2d = pos.toSkewCylCoords.offset(-d - 0.001f, 0, 0).toCylCoords

    assert(detector.collides(box1, pos, box2, pos2a))
    assert(!detector.collides(box1, pos, box2, pos2b))
    assert(detector.collides(box1, pos, box2, pos2c))
    assert(!detector.collides(box1, pos, box2, pos2d))
  }

  test("collides should work in the w-direction") {
    val provider = new FakeWorldProvider(37)
    val world = FakeBlocksInWorld.empty(provider)
    val detector = new CollisionDetector(world)

    val d = box1.smallRadius + box2.smallRadius

    val pos2a = pos.toSkewCylCoords.offset(d - 0.001, 0, -(d - 0.001)).toCylCoords
    val pos2b = pos.toSkewCylCoords.offset(d + 0.001f, 0, -(d + 0.001f)).toCylCoords
    val pos2c = pos.toSkewCylCoords.offset(-d + 0.001f, 0, -(-d + 0.001f)).toCylCoords
    val pos2d = pos.toSkewCylCoords.offset(-d - 0.001f, 0, -(-d - 0.001f)).toCylCoords

    assert(detector.collides(box1, pos, box2, pos2a))
    assert(!detector.collides(box1, pos, box2, pos2b))
    assert(detector.collides(box1, pos, box2, pos2c))
    assert(!detector.collides(box1, pos, box2, pos2d))
  }

  def checkCollision(
      detector: CollisionDetector,
      box: HexBox,
      pos: SkewCylCoords,
      velocity: SkewCylCoords.Offset,
      shouldStopAfter: Option[SkewCylCoords.Offset]
  ): Unit = {
    val (newPos, newVel) = detector.positionAndVelocityAfterCollision(
      box,
      pos.toCylCoords.toVector3d,
      velocity.toCylCoordsOffset.toVector3d
    )

    val (expectedNewPos, expectedNewVel) = shouldStopAfter match {
      case Some(coords) =>
        (pos.offset(coords), SkewCylCoords.Offset(0, 0, 0))
      case None =>
        (pos.offset(velocity), velocity)
    }

    assertEqualsDouble(newPos.distance(expectedNewPos.toCylCoords.toVector3d), 0.0, 1e-6)
    assertEqualsDouble(newVel.distance(expectedNewVel.toCylCoordsOffset.toVector3d), 0.0, 1e-6)
  }

  test("positionAndVelocityAfterCollision should do nothing if velocity is 0") {
    val coords = BlockRelWorld(17, -48, 3)

    val provider = new FakeWorldProvider(37)
    val world = FakeBlocksInWorld.withBlocks(
      provider,
      Map(coords -> BlockState.Air)
    )

    // Ensure the chunk is loaded
    assert(world.getChunk(coords.getChunkRelWorld).isDefined)

    // Check for collision
    val detector = new CollisionDetector(world)
    assertEquals(
      detector.positionAndVelocityAfterCollision(
        box1,
        BlockCoords(coords).toCylCoords.toVector3d,
        new Vector3d
      ),
      (BlockCoords(coords).toCylCoords.toVector3d, new Vector3d)
    )
  }

  test("positionAndVelocityAfterCollision should do nothing if inside a block") {
    val coords = BlockRelWorld(17, -48, 3)

    val provider = new FakeWorldProvider(37)
    val world = FakeBlocksInWorld.withBlocks(
      provider,
      Map(coords -> BlockState(Block.Dirt))
    )
    val detector = new CollisionDetector(world)

    // Ensure the chunk is loaded
    assert(world.getChunk(coords.getChunkRelWorld).isDefined)

    // Check for collision (it should not move)
    val position = BlockCoords(coords).toSkewCylCoords
    val velocity = SkewCylCoords.Offset(3.2, 1.4, -0.9)
    val zeroMovement = SkewCylCoords.Offset(0, 0, 0)
    checkCollision(detector, box1, position, velocity, Some(zeroMovement))
  }

  test("positionAndVelocityAfterCollision should do nothing if the chunk is not loaded") {
    val provider = new FakeWorldProvider(37)
    val world = FakeBlocksInWorld.empty(provider)
    val detector = new CollisionDetector(world)

    // Ensure the chunk is NOT loaded
    val coords = BlockRelWorld(17, -48, 3)
    world.provideColumn(coords.getColumnRelWorld)
    assertEquals(world.getChunk(coords.getChunkRelWorld), None)

    // Check for collision (it should not move)
    val position = BlockCoords(coords).toSkewCylCoords
    val velocity = SkewCylCoords.Offset(3.2, 1.4, -0.9)
    val zeroMovement = SkewCylCoords.Offset(0, 0, 0)
    checkCollision(detector, box1, position, velocity, Some(zeroMovement))
  }

  test("positionAndVelocityAfterCollision should add velocity to position if there is no collision") {
    val coords = BlockRelWorld(1, -7, 7)

    val provider = new FakeWorldProvider(37)
    val world = FakeBlocksInWorld.withBlocks(
      provider,
      Map.from(
        for {
          dz <- -1 to 1
          dy <- -1 to 1
          dx <- -1 to 1
        } yield coords.offset(dx, dy, dz) -> BlockState.Air
      )
    )

    // Ensure the chunk is loaded
    assert(world.getChunk(coords.getChunkRelWorld).isDefined)

    // Check for collision
    val box = new HexBox(0.15f, 0.1f, 0.3f)
    val velocity = BlockCoords.Offset(0.2, 0.39, 0.71).toSkewCylCoordsOffset
    val detector = new CollisionDetector(world)

    checkCollision(detector, box, BlockCoords(coords).toSkewCylCoords, velocity, None)
  }

  test("positionAndVelocityAfterCollision should work in the x-direction") {
    val coords = BlockRelWorld(5, 7, 9)

    val provider = new FakeWorldProvider(37)
    val world = FakeBlocksInWorld.withBlocks(
      provider,
      Map.from(
        (-1 to 3).map: dx =>
          val b = dx match
            case -1 | 3 => BlockState(Block.Dirt)
            case _      => BlockState.Air
          coords.offset(dx, 0, 0) -> b
      )
    )

    // Ensure the chunk is loaded
    assert(world.getChunk(coords.getChunkRelWorld).isDefined)

    val detector = new CollisionDetector(world)
    val box = new HexBox(0.15f, 0.1f, 0.3f)

    // Check for collision forward
    val back = BlockCoords(coords).toSkewCylCoords // right at the beginning of the first Air
    val forwardMax = BlockCoords
      .Offset(2.5, 0, 0)
      .toSkewCylCoordsOffset
      .offset(-box.smallRadius, 0, 0) // maximal movement from back to upper Dirt

    // Just before colliding
    checkCollision(detector, box, back, forwardMax.offset(-0.001, 0, 0), None)

    // Too far
    checkCollision(detector, box, back, forwardMax.offset(0.001, 0, 0), Some(forwardMax))

    // Check for collision backward
    val front =
      BlockCoords(coords).offset(2, 0, 0).toSkewCylCoords // right at the beginning of the last Air
    val backwardMax = BlockCoords
      .Offset(-2.5, 0, 0)
      .toSkewCylCoordsOffset
      .offset(box.smallRadius, 0, 0) // maximal movement from front to lower Dirt

    // Just before colliding
    checkCollision(detector, box, front, backwardMax.offset(0.001, 0, 0), None)

    // Too far
    checkCollision(detector, box, front, backwardMax.offset(-0.001, 0, 0), Some(backwardMax))
  }

  test("positionAndVelocityAfterCollision should work in the y-direction") {
    val coords = BlockRelWorld(5, 7, 9)

    val provider = new FakeWorldProvider(37)
    val world = FakeBlocksInWorld.withBlocks(
      provider,
      Map.from(
        (-1 to 3).map: dy =>
          val b = dy match
            case -1 | 3 => BlockState(Block.Dirt)
            case _      => BlockState.Air
          coords.offset(0, dy, 0) -> b
      )
    )

    // Ensure the chunk is loaded
    assert(world.getChunk(coords.getChunkRelWorld).isDefined)

    val detector = new CollisionDetector(world)
    val box = new HexBox(0.15f, 0.1f, 0.3f)

    // Check for collision up
    val bottom = BlockCoords(coords).toSkewCylCoords // right at the beginning of the first Air
    val upMax = BlockCoords
      .Offset(0, 3, 0)
      .toSkewCylCoordsOffset
      .offset(0, -0.3, 0) // maximal movement from back to upper Dirt

    // Just before colliding
    checkCollision(detector, box, bottom, upMax.offset(0, -0.001, 0), None)

    // Too far
    checkCollision(detector, box, bottom, upMax.offset(0, 0.001, 0), Some(upMax))

    // Check for collision down
    val top =
      BlockCoords(coords)
        .offset(0, 2.0, 0)
        .toSkewCylCoords // right at the beginning of the last Air
    val downMax = BlockCoords
      .Offset(0, -2, 0)
      .toSkewCylCoordsOffset
      .offset(0, -0.1, 0) // maximal movement from front to lower Dirt

    // Just before colliding
    checkCollision(detector, box, top, downMax.offset(0, 0.001, 0), None)

    // Too far
    checkCollision(detector, box, top, downMax.offset(0, -0.001, 0), Some(downMax))
  }

  test("positionAndVelocityAfterCollision should work in the z-direction") {
    val coords = BlockRelWorld(5, 7, 9)

    val provider = new FakeWorldProvider(37)
    val world = FakeBlocksInWorld.withBlocks(
      provider,
      Map.from(
        (-1 to 3).map: dz =>
          val b = dz match
            case -1 | 3 => BlockState(Block.Dirt)
            case _      => BlockState.Air
          coords.offset(0, 0, dz) -> b
      )
    )

    // Ensure the chunk is loaded
    assert(world.getChunk(coords.getChunkRelWorld).isDefined)

    val detector = new CollisionDetector(world)
    val box = new HexBox(0.15f, 0.1f, 0.3f)

    // Check for collision forward
    val back = BlockCoords(coords).toSkewCylCoords // right at the beginning of the first Air
    val forwardMax = BlockCoords
      .Offset(0, 0, 2.5)
      .toSkewCylCoordsOffset
      .offset(0, 0, -box.smallRadius) // maximal movement from back to upper Dirt

    // Just before colliding
    checkCollision(detector, box, back, forwardMax.offset(0, 0, -0.001), None)

    // Too far
    checkCollision(detector, box, back, forwardMax.offset(0, 0, 0.001), Some(forwardMax))

    // Check for collision backward
    val front =
      BlockCoords(coords).offset(0, 0, 2).toSkewCylCoords // right at the beginning of the last Air
    val backwardMax = BlockCoords
      .Offset(0, 0, -2.5)
      .toSkewCylCoordsOffset
      .offset(0, 0, box.smallRadius) // maximal movement from front to lower Dirt

    // Just before colliding
    checkCollision(detector, box, front, backwardMax.offset(0, 0, 0.001), None)

    // Too far
    checkCollision(detector, box, front, backwardMax.offset(0, 0, -0.001), Some(backwardMax))
  }

  test("positionAndVelocityAfterCollision should work in the w-direction") {
    val coords = BlockRelWorld(5, 7, 9)

    val provider = new FakeWorldProvider(37)
    val world = FakeBlocksInWorld.withBlocks(
      provider,
      Map.from(
        (-1 to 3).map: dw =>
          val b = dw match
            case -1 | 3 => BlockState(Block.Dirt)
            case _      => BlockState.Air
          coords.offset(dw, 0, -dw) -> b
      )
    )

    // Ensure the chunk is loaded
    assert(world.getChunk(coords.getChunkRelWorld).isDefined)

    val detector = new CollisionDetector(world)
    val box = new HexBox(0.15f, 0.1f, 0.3f)

    // Check for collision forward
    val back = BlockCoords(coords).toSkewCylCoords // right at the beginning of the first Air
    val forwardMax = BlockCoords
      .Offset(2.5, 0, -2.5)
      .toSkewCylCoordsOffset
      .offset(-box.smallRadius, 0, box.smallRadius) // maximal movement from back to upper Dirt

    // Just before colliding
    checkCollision(detector, box, back, forwardMax.offset(-0.001, 0, 0.001), None)

    // Too far
    checkCollision(detector, box, back, forwardMax.offset(0.001, 0, -0.001), Some(forwardMax))

    // Check for collision backward
    val front =
      BlockCoords(coords).offset(2, 0, -2).toSkewCylCoords // right at the beginning of the last Air
    val backwardMax = BlockCoords
      .Offset(-2.5, 0, 2.5)
      .toSkewCylCoordsOffset
      .offset(box.smallRadius, 0, -box.smallRadius) // maximal movement from front to lower Dirt

    // Just before colliding
    checkCollision(detector, box, front, backwardMax.offset(0.001, 0, -0.001), None)

    // Too far
    checkCollision(detector, box, front, backwardMax.offset(-0.001, 0, 0.001), Some(backwardMax))
  }
}
