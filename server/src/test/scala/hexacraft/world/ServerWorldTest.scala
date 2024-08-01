package hexacraft.world

import hexacraft.server.ServerWorld
import hexacraft.world.block.{Block, BlockState}
import hexacraft.world.chunk.Chunk
import hexacraft.world.coord.{BlockCoords, BlockRelWorld, ChunkRelWorld, CylCoords}
import hexacraft.world.entity.{BoundsComponent, Entity, MotionComponent, TransformComponent}

import munit.FunSuite

import java.util.UUID

class ServerWorldTest extends FunSuite {
  given CylinderSize = CylinderSize(8)

  test("the world should not crash") {
    val provider = new FakeWorldProvider(1234)
    val world = ServerWorld(provider, provider.getWorldInfo)
    val camera = new Camera(new CameraProjection(70, 1.6f, 0.01f, 1000f))

    world.tick(Seq(camera), Seq(), Seq())

    val cCoords = ChunkRelWorld(3, 7, -4)

    // Set a chunk in the world
    assertEquals(world.getChunk(cCoords), None)
    val col = world.provideColumn(cCoords.getColumnRelWorld)
    val chunk = Chunk.fromGenerator(cCoords, col, WorldGenerator(provider.getWorldInfo.gen))
    world.setChunk(cCoords, chunk)
    assertEquals(world.getChunk(cCoords), Some(chunk))

    // Set a block in the chunk
    val bCoords = BlockRelWorld(5, 1, 3, cCoords)
    assertEquals(world.getBlock(bCoords), BlockState.Air)
    world.setBlock(bCoords, BlockState(Block.Stone, 2))
    assertEquals(world.getBlock(bCoords), BlockState(Block.Stone, 2))

    // Clean up
    world.unload()
  }

  test("the world should decorate new chunks") {
    val provider = new FakeWorldProvider(1234)
    val world = ServerWorld(provider, provider.getWorldInfo)

    val chunkCoords = ChunkRelWorld(3, -1, -4) // this chunk contains the ground

    // Set a chunk in the world
    val col = world.provideColumn(chunkCoords.getColumnRelWorld)
    val chunk = Chunk.fromGenerator(chunkCoords, col, WorldGenerator(provider.getWorldInfo.gen))
    world.setChunk(chunkCoords, chunk)

    // The planner should have decorated the chunk
    assert(chunk.isDecorated)

    // There should be a tree in the chunk
    assert(chunk.blocks.exists(s => s.block.blockType == Block.OakLog))

    // Clean up
    world.unload()
  }

  test("the world should load chunks close to the camera") {
    val provider = new FakeWorldProvider(1234)
    val world = ServerWorld(provider, provider.getWorldInfo)
    val camera = new Camera(new CameraProjection(70, 1.6f, 0.01f, 1000f))

    val cCoords = ChunkRelWorld(3, 7, -4)
    camera.setPosition(BlockCoords(BlockRelWorld(8, 8, 8, cCoords)).toCylCoords.toVector3d)

    // The chunk should be unloaded from the beginning
    assert(world.getChunk(cCoords).isEmpty)

    // Run the game a bit
    world.tick(Seq(camera), Seq(cCoords), Seq())
    Thread.sleep(20)
    world.tick(Seq(camera), Seq(), Seq())

    // The chunk should be loaded
    assert(world.getChunk(cCoords).isDefined)

    // Clean up
    world.unload()
  }

  test("the world should unload chunks far from the camera") {
    val provider = new FakeWorldProvider(1234)
    val world = ServerWorld(provider, provider.getWorldInfo)
    val camera = new Camera(new CameraProjection(70, 1.6f, 0.01f, 1000f))

    val cCoords = ChunkRelWorld(3, 7, -4)
    camera.setPosition(BlockCoords(BlockRelWorld(8, 8, 8, cCoords)).toCylCoords.toVector3d)

    // Run the game a bit
    world.tick(Seq(camera), Seq(cCoords), Seq())
    Thread.sleep(20)
    world.tick(Seq(camera), Seq(), Seq())

    // The chunk should be loaded
    assert(world.getChunk(cCoords).isDefined)

    // Move far away
    camera.setPosition(BlockCoords(BlockRelWorld(8, 8, 8, cCoords.offset(100, 0, 0))).toCylCoords.toVector3d)

    // Run the game a bit
    world.tick(Seq(camera), Seq(), Seq(cCoords))
    Thread.sleep(20)
    world.tick(Seq(camera), Seq(), Seq())

    // The chunk should be unloaded
    assert(world.getChunk(cCoords).isEmpty)

    // Clean up
    world.unload()
  }

  test("the world should allow entities to be added to and removed from a loaded chunk") {
    val provider = new FakeWorldProvider(1234)
    val world = ServerWorld(provider, provider.getWorldInfo)
    val camera = new Camera(new CameraProjection(70, 1.6f, 0.01f, 1000f))

    val entityPosition = CylCoords(1, 2, 3)

    // Make sure the chunk is loaded
    camera.setPosition(entityPosition.toVector3d)
    world.tick(Seq(camera), Seq(ChunkRelWorld(0, 0, 0)), Seq())
    Thread.sleep(20)
    world.tick(Seq(camera), Seq(), Seq())

    assert(world.getChunk(ChunkRelWorld(0, 0, 0)).isDefined)

    val entity = Entity(
      UUID.randomUUID(),
      "scorpion",
      Seq(TransformComponent(entityPosition), MotionComponent(), BoundsComponent(HexBox(0.5f, 0, 0.5f)))
    )

    world.addEntity(entity)

    val pos1 = entity.transform.position
    world.tick(Seq(camera), Seq(), Seq())
    val pos2 = entity.transform.position
    assertNotEquals(pos1, pos2)

    world.removeEntity(entity)

    world.tick(Seq(camera), Seq(), Seq())
    val pos3 = entity.transform.position
    assertEquals(pos2, pos3)

    world.addEntity(entity)

    world.tick(Seq(camera), Seq(), Seq())
    val pos4 = entity.transform.position
    assertNotEquals(pos3, pos4)

    world.removeAllEntities()

    world.tick(Seq(camera), Seq(), Seq())
    val pos5 = entity.transform.position
    assertEquals(pos4, pos5)
  }
}
