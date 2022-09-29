package com.martomate.hexacraft.world.collision

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.{BlockState, Blocks, HexBox}
import com.martomate.hexacraft.world.chunk.Chunk
import com.martomate.hexacraft.world.coord.fp.{BlockCoords, CylCoords, SkewCylCoords}
import com.martomate.hexacraft.world.coord.integer.{BlockRelWorld, Offset}
import com.martomate.hexacraft.world.{CollisionDetector, FakeBlocksInWorld, FakeWorldProvider}
import org.joml.Vector3d
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CollisionDetectorTest extends AnyFlatSpec with Matchers {
  implicit val cylSize: CylinderSize = new CylinderSize(8)

  private val box1 = new HexBox(0.4f, 0.1f, 0.3f)
  private val box2 = new HexBox(0.5f, 0.15f, 0.45f)
  private val pos: CylCoords = BlockCoords(1, 27, -3).toCylCoords

  "collides" should "return true for boxes at the same location" in {
    val provider = new FakeWorldProvider(37)
    val world = FakeBlocksInWorld.empty(provider)
    val detector = new CollisionDetector(world)

    detector.collides(box1, pos, box2, pos) shouldBe true
  }

  it should "work in the y-direction" in {
    val provider = new FakeWorldProvider(37)
    val world = FakeBlocksInWorld.empty(provider)
    val detector = new CollisionDetector(world)

    detector.collides(
      box1,
      pos,
      box2,
      pos.toSkewCylCoords.offset(0, box1.top - box2.bottom - 0.001f, 0).toCylCoords
    ) shouldBe true
    detector.collides(
      box1,
      pos,
      box2,
      pos.toSkewCylCoords.offset(0, box1.top - box2.bottom + 0.001f, 0).toCylCoords
    ) shouldBe false
    detector.collides(
      box1,
      pos,
      box2,
      pos.toSkewCylCoords.offset(0, box1.bottom - box2.top + 0.001f, 0).toCylCoords
    ) shouldBe true
    detector.collides(
      box1,
      pos,
      box2,
      pos.toSkewCylCoords.offset(0, box1.bottom - box2.top - 0.001f, 0).toCylCoords
    ) shouldBe false
  }

  it should "work in the z-direction" in {
    val provider = new FakeWorldProvider(37)
    val world = FakeBlocksInWorld.empty(provider)
    val detector = new CollisionDetector(world)

    detector.collides(
      box1,
      pos,
      box2,
      pos.toSkewCylCoords.offset(0, 0, box1.smallRadius + box2.smallRadius - 0.001).toCylCoords
    ) shouldBe true
    detector.collides(
      box1,
      pos,
      box2,
      pos.toSkewCylCoords.offset(0, 0, box1.smallRadius + box2.smallRadius + 0.001f).toCylCoords
    ) shouldBe false
    detector.collides(
      box1,
      pos,
      box2,
      pos.toSkewCylCoords.offset(0, 0, -box1.smallRadius - box2.smallRadius + 0.001f).toCylCoords
    ) shouldBe true
    detector.collides(
      box1,
      pos,
      box2,
      pos.toSkewCylCoords.offset(0, 0, -box1.smallRadius - box2.smallRadius - 0.001f).toCylCoords
    ) shouldBe false
  }

  it should "work in the x-direction" in {
    val provider = new FakeWorldProvider(37)
    val world = FakeBlocksInWorld.empty(provider)
    val detector = new CollisionDetector(world)

    detector.collides(
      box1,
      pos,
      box2,
      pos.toSkewCylCoords.offset(box1.smallRadius + box2.smallRadius - 0.001, 0, 0).toCylCoords
    ) shouldBe true
    detector.collides(
      box1,
      pos,
      box2,
      pos.toSkewCylCoords.offset(box1.smallRadius + box2.smallRadius + 0.001f, 0, 0).toCylCoords
    ) shouldBe false
    detector.collides(
      box1,
      pos,
      box2,
      pos.toSkewCylCoords.offset(-box1.smallRadius - box2.smallRadius + 0.001f, 0, 0).toCylCoords
    ) shouldBe true
    detector.collides(
      box1,
      pos,
      box2,
      pos.toSkewCylCoords.offset(-box1.smallRadius - box2.smallRadius - 0.001f, 0, 0).toCylCoords
    ) shouldBe false
  }

  it should "work in the w-direction" in {
    val provider = new FakeWorldProvider(37)
    val world = FakeBlocksInWorld.empty(provider)
    val detector = new CollisionDetector(world)

    detector.collides(
      box1,
      pos,
      box2,
      pos.toSkewCylCoords
        .offset(
          box1.smallRadius + box2.smallRadius - 0.001,
          0,
          -(box1.smallRadius + box2.smallRadius - 0.001)
        )
        .toCylCoords
    ) shouldBe true
    detector.collides(
      box1,
      pos,
      box2,
      pos.toSkewCylCoords
        .offset(
          box1.smallRadius + box2.smallRadius + 0.001f,
          0,
          -(box1.smallRadius + box2.smallRadius + 0.001f)
        )
        .toCylCoords
    ) shouldBe false
    detector.collides(
      box1,
      pos,
      box2,
      pos.toSkewCylCoords
        .offset(
          -box1.smallRadius - box2.smallRadius + 0.001f,
          0,
          -(-box1.smallRadius - box2.smallRadius + 0.001f)
        )
        .toCylCoords
    ) shouldBe true
    detector.collides(
      box1,
      pos,
      box2,
      pos.toSkewCylCoords
        .offset(
          -box1.smallRadius - box2.smallRadius - 0.001f,
          0,
          -(-box1.smallRadius - box2.smallRadius - 0.001f)
        )
        .toCylCoords
    ) shouldBe false
  }

  def checkCollision(
      detector: CollisionDetector,
      box: HexBox,
      pos: SkewCylCoords,
      velocity: SkewCylCoords,
      shouldStopAfter: Option[SkewCylCoords]
  ): Unit = {
    val (newPos, newVel) = detector.positionAndVelocityAfterCollision(
      box,
      pos.toCylCoords.toVector3d,
      velocity.toCylCoords.toVector3d
    )

    val (expectedNewPos, expectedNewVel) = shouldStopAfter match {
      case Some(coords) =>
        (pos + coords, SkewCylCoords(0, 0, 0, fixZ = false))
      case None =>
        (pos + velocity, velocity)
    }

    newPos.distance(expectedNewPos.toCylCoords.toVector3d) shouldBe 0.0 +- 1e-6
    newVel.distance(expectedNewVel.toCylCoords.toVector3d) shouldBe 0.0 +- 1e-6
  }

  "positionAndVelocityAfterCollision" should "do nothing if velocity is 0" in {
    val provider = new FakeWorldProvider(37)
    val world = FakeBlocksInWorld.empty(provider)
    val detector = new CollisionDetector(world)

    // Ensure the chunk is loaded
    val coords = BlockRelWorld(17, -48, 3)
    val chunk = Chunk(coords.getChunkRelWorld, world, provider)
    world.provideColumn(coords.getColumnRelWorld).setChunk(chunk)

    // Check for collision
    detector.positionAndVelocityAfterCollision(
      box1,
      BlockCoords(coords).toCylCoords.toVector3d,
      new Vector3d
    ) shouldBe (BlockCoords(coords).toCylCoords.toVector3d, new Vector3d)
  }

  it should "do nothing if inside a block" in {
    val provider = new FakeWorldProvider(37)
    val world = FakeBlocksInWorld.empty(provider)
    val detector = new CollisionDetector(world)

    // Ensure the chunk is loaded
    val coords = BlockRelWorld(17, -48, 3)
    val chunk = Chunk(coords.getChunkRelWorld, world, provider)
    world.provideColumn(coords.getColumnRelWorld).setChunk(chunk)

    // Place a block
    chunk.setBlock(coords.getBlockRelChunk, BlockState(Blocks.Dirt))

    // Check for collision (it should not move)
    val position = BlockCoords(coords).toSkewCylCoords
    val velocity = SkewCylCoords(3.2, 1.4, -0.9, fixZ = false)
    val zeroMovement = SkewCylCoords(0, 0, 0, fixZ = false)
    checkCollision(detector, box1, position, velocity, Some(zeroMovement))
  }

  it should "do nothing if the chunk is not loaded" in {
    val provider = new FakeWorldProvider(37)
    val world = FakeBlocksInWorld.empty(provider)
    val detector = new CollisionDetector(world)

    // Ensure the chunk is NOT loaded
    val coords = BlockRelWorld(17, -48, 3)
    world.provideColumn(coords.getColumnRelWorld)
    world.getChunk(coords.getChunkRelWorld) shouldBe None

    // Check for collision (it should not move)
    val position = BlockCoords(coords).toSkewCylCoords
    val velocity = SkewCylCoords(3.2, 1.4, -0.9, fixZ = false)
    val zeroMovement = SkewCylCoords(0, 0, 0, fixZ = false)
    checkCollision(detector, box1, position, velocity, Some(zeroMovement))
  }

  it should "add velocity to position if there is no collision" in {
    val provider = new FakeWorldProvider(37)
    val world = FakeBlocksInWorld.empty(provider)
    val detector = new CollisionDetector(world)

    // Ensure the chunk is loaded
    val coords = BlockRelWorld(1, -7, 7)
    val chunk = Chunk(coords.getChunkRelWorld, world, provider)
    world.provideColumn(coords.getColumnRelWorld).setChunk(chunk)

    // Clear the surrounding blocks
    for {
      dz <- -1 to 1
      dy <- -1 to 1
      dx <- -1 to 1
    } chunk.setBlock(coords.offset(dx, dy, dz).getBlockRelChunk, BlockState.Air)

    // Check for collision
    val box = new HexBox(0.15f, 0.1f, 0.3f)
    val velocity = BlockCoords(0.2, 0.39, 0.71).toSkewCylCoords
    checkCollision(detector, box, BlockCoords(coords).toSkewCylCoords, velocity, None)
  }

  it should "work in the x-direction" in {
    val provider = new FakeWorldProvider(37)
    val world = FakeBlocksInWorld.empty(provider)
    val detector = new CollisionDetector(world)

    // Ensure the chunk is loaded
    val coords = BlockRelWorld(5, 7, 9)
    val chunk = Chunk(coords.getChunkRelWorld, world, provider)
    world.provideColumn(coords.getColumnRelWorld).setChunk(chunk)

    // Set blocks: Dirt, 3 Air, Dirt
    chunk.setBlock(coords.offset(Offset(3, 0, 0)).getBlockRelChunk, BlockState(Blocks.Dirt))
    for (off <- Seq(0, 1, 2).map(dx => Offset(dx, 0, 0)))
      chunk.setBlock(coords.offset(off).getBlockRelChunk, BlockState.Air)
    chunk.setBlock(coords.offset(Offset(-1, 0, 0)).getBlockRelChunk, BlockState(Blocks.Dirt))

    val box = new HexBox(0.15f, 0.1f, 0.3f)

    // Check for collision forward
    val back = BlockCoords(coords).toSkewCylCoords // right at the beginning of the first Air
    val forwardMax = BlockCoords(2.5, 0, 0, fixZ = false).toSkewCylCoords
      .offset(-box.smallRadius, 0, 0) // maximal movement from back to upper Dirt

    // Just before colliding
    checkCollision(detector, box, back, forwardMax.offset(-0.001, 0, 0), None)

    // Too far
    checkCollision(detector, box, back, forwardMax.offset(0.001, 0, 0), Some(forwardMax))

    // Check for collision backward
    val front =
      BlockCoords(coords).offset(2, 0, 0).toSkewCylCoords // right at the beginning of the last Air
    val backwardMax = BlockCoords(-2.5, 0, 0, fixZ = false).toSkewCylCoords
      .offset(box.smallRadius, 0, 0) // maximal movement from front to lower Dirt

    // Just before colliding
    checkCollision(detector, box, front, backwardMax.offset(0.001, 0, 0), None)

    // Too far
    checkCollision(detector, box, front, backwardMax.offset(-0.001, 0, 0), Some(backwardMax))
  }

  it should "work in the y-direction" in {
    val provider = new FakeWorldProvider(37)
    val world = FakeBlocksInWorld.empty(provider)
    val detector = new CollisionDetector(world)

    // Ensure the chunk is loaded
    val coords = BlockRelWorld(5, 7, 9)
    val chunk = Chunk(coords.getChunkRelWorld, world, provider)
    world.provideColumn(coords.getColumnRelWorld).setChunk(chunk)

    // Set blocks: Dirt, 3 Air, Dirt
    chunk.setBlock(coords.offset(Offset(0, 3, 0)).getBlockRelChunk, BlockState(Blocks.Dirt))
    for (off <- Seq(0, 1, 2).map(dy => Offset(0, dy, 0)))
      chunk.setBlock(coords.offset(off).getBlockRelChunk, BlockState.Air)
    chunk.setBlock(coords.offset(Offset(0, -1, 0)).getBlockRelChunk, BlockState(Blocks.Dirt))

    val box = new HexBox(0.15f, 0.1f, 0.3f)

    // Check for collision up
    val bottom = BlockCoords(coords).toSkewCylCoords // right at the beginning of the first Air
    val upMax = BlockCoords(0, 3, 0, fixZ = false).toSkewCylCoords
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
    val downMax = BlockCoords(0, -2, 0, fixZ = false).toSkewCylCoords
      .offset(0, -0.1, 0) // maximal movement from front to lower Dirt

    // Just before colliding
    checkCollision(detector, box, top, downMax.offset(0, 0.001, 0), None)

    // Too far
    checkCollision(detector, box, top, downMax.offset(0, -0.001, 0), Some(downMax))
  }

  it should "work in the z-direction" in {
    val provider = new FakeWorldProvider(37)
    val world = FakeBlocksInWorld.empty(provider)
    val detector = new CollisionDetector(world)

    // Ensure the chunk is loaded
    val coords = BlockRelWorld(5, 7, 9)
    val chunk = Chunk(coords.getChunkRelWorld, world, provider)
    world.provideColumn(coords.getColumnRelWorld).setChunk(chunk)

    // Set blocks: Dirt, 3 Air, Dirt
    chunk.setBlock(coords.offset(Offset(0, 0, 3)).getBlockRelChunk, BlockState(Blocks.Dirt))
    for (off <- Seq(0, 1, 2).map(dz => Offset(0, 0, dz)))
      chunk.setBlock(coords.offset(off).getBlockRelChunk, BlockState.Air)
    chunk.setBlock(coords.offset(Offset(0, 0, -1)).getBlockRelChunk, BlockState(Blocks.Dirt))

    val box = new HexBox(0.15f, 0.1f, 0.3f)

    // Check for collision forward
    val back = BlockCoords(coords).toSkewCylCoords // right at the beginning of the first Air
    val forwardMax = BlockCoords(0, 0, 2.5, fixZ = false).toSkewCylCoords
      .offset(0, 0, -box.smallRadius) // maximal movement from back to upper Dirt

    // Just before colliding
    checkCollision(detector, box, back, forwardMax.offset(0, 0, -0.001), None)

    // Too far
    checkCollision(detector, box, back, forwardMax.offset(0, 0, 0.001), Some(forwardMax))

    // Check for collision backward
    val front =
      BlockCoords(coords).offset(0, 0, 2).toSkewCylCoords // right at the beginning of the last Air
    val backwardMax = BlockCoords(0, 0, -2.5, fixZ = false).toSkewCylCoords
      .offset(0, 0, box.smallRadius) // maximal movement from front to lower Dirt

    // Just before colliding
    checkCollision(detector, box, front, backwardMax.offset(0, 0, 0.001), None)

    // Too far
    checkCollision(detector, box, front, backwardMax.offset(0, 0, -0.001), Some(backwardMax))
  }

  it should "work in the w-direction" in {
    val provider = new FakeWorldProvider(37)
    val world = FakeBlocksInWorld.empty(provider)
    val detector = new CollisionDetector(world)

    // Ensure the chunk is loaded
    val coords = BlockRelWorld(5, 7, 9)
    val chunk = Chunk(coords.getChunkRelWorld, world, provider)
    world.provideColumn(coords.getColumnRelWorld).setChunk(chunk)

    // Set blocks: Dirt, 3 Air, Dirt
    chunk.setBlock(coords.offset(Offset(3, 0, -3)).getBlockRelChunk, BlockState(Blocks.Dirt))
    for (off <- Seq(0, 1, 2).map(dw => Offset(dw, 0, -dw)))
      chunk.setBlock(coords.offset(off).getBlockRelChunk, BlockState.Air)
    chunk.setBlock(coords.offset(Offset(-1, 0, 1)).getBlockRelChunk, BlockState(Blocks.Dirt))

    val box = new HexBox(0.15f, 0.1f, 0.3f)

    // Check for collision forward
    val back = BlockCoords(coords).toSkewCylCoords // right at the beginning of the first Air
    val forwardMax = BlockCoords(2.5, 0, -2.5, fixZ = false).toSkewCylCoords
      .offset(-box.smallRadius, 0, box.smallRadius) // maximal movement from back to upper Dirt

    // Just before colliding
    checkCollision(detector, box, back, forwardMax.offset(-0.001, 0, 0.001), None)

    // Too far
    checkCollision(detector, box, back, forwardMax.offset(0.001, 0, -0.001), Some(forwardMax))

    // Check for collision backward
    val front =
      BlockCoords(coords).offset(2, 0, -2).toSkewCylCoords // right at the beginning of the last Air
    val backwardMax = BlockCoords(-2.5, 0, 2.5, fixZ = false).toSkewCylCoords
      .offset(box.smallRadius, 0, -box.smallRadius) // maximal movement from front to lower Dirt

    // Just before colliding
    checkCollision(detector, box, front, backwardMax.offset(0.001, 0, -0.001), None)

    // Too far
    checkCollision(detector, box, front, backwardMax.offset(-0.001, 0, 0.001), Some(backwardMax))
  }
}
