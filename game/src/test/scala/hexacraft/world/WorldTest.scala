package hexacraft.world

import hexacraft.world.block.{Block, BlockState}
import hexacraft.world.camera.{Camera, CameraProjection}
import hexacraft.world.chunk.Chunk
import hexacraft.world.coord.fp.{BlockCoords, CylCoords}
import hexacraft.world.coord.integer.{BlockRelWorld, ChunkRelWorld}
import hexacraft.world.entity.{Entity, EntityBaseData, EntityModelLoader}

import munit.FunSuite

class WorldTest extends FunSuite {
  given CylinderSize = CylinderSize(8)

  test("the world should not crash") {
    val provider = new FakeWorldProvider(1234)
    val world = World(provider, provider.getWorldInfo, new EntityModelLoader)
    val camera = new Camera(new CameraProjection(70, 1.6f, 0.01f, 1000f))

    world.tick(camera)

    val cCoords = ChunkRelWorld(3, 7, -4)

    // Set a chunk in the world
    assertEquals(world.getChunk(cCoords), None)
    val chunk = Chunk(cCoords, world, provider)
    world.setChunk(chunk)
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
    val world = World(provider, provider.getWorldInfo, new EntityModelLoader)

    val chunkCoords = ChunkRelWorld(3, -1, -4) // this chunk contains the ground

    // Set a chunk in the world
    val chunk = Chunk(chunkCoords, world, provider)
    world.setChunk(chunk)

    // The planner should have decorated the chunk
    assert(chunk.isDecorated)

    // There should be a tree in the chunk
    assert(chunk.blocks.exists(s => s.block.blockType == Block.Log))

    // Clean up
    world.unload()
  }

  test("the world should load chunks close to the camera") {
    val provider = new FakeWorldProvider(1234)
    val world = World(provider, provider.getWorldInfo, new EntityModelLoader)
    val camera = new Camera(new CameraProjection(70, 1.6f, 0.01f, 1000f))

    val cCoords = ChunkRelWorld(3, 7, -4)
    camera.setPosition(BlockCoords(BlockRelWorld(8, 8, 8, cCoords)).toCylCoords.toVector3d)

    // The chunk should be unloaded from the beginning
    assert(world.getChunk(cCoords).isEmpty)

    // Run the game a bit
    world.tick(camera)
    Thread.sleep(10)
    world.tick(camera)

    // The chunk should be loaded
    assert(world.getChunk(cCoords).isDefined)

    // Clean up
    world.unload()
  }

  test("the world should unload chunks far from the camera") {
    val provider = new FakeWorldProvider(1234)
    val world = World(provider, provider.getWorldInfo, new EntityModelLoader)
    val camera = new Camera(new CameraProjection(70, 1.6f, 0.01f, 1000f))

    val cCoords = ChunkRelWorld(3, 7, -4)
    camera.setPosition(BlockCoords(BlockRelWorld(8, 8, 8, cCoords)).toCylCoords.toVector3d)

    // Run the game a bit
    world.tick(camera)
    Thread.sleep(10)
    world.tick(camera)

    // The chunk should be loaded
    assert(world.getChunk(cCoords).isDefined)

    // Move far away
    camera.setPosition(BlockCoords(BlockRelWorld(8, 8, 8, cCoords.offset(100, 0, 0))).toCylCoords.toVector3d)

    // Run the game a bit
    world.tick(camera)
    Thread.sleep(10)
    world.tick(camera)

    // The chunk should be unloaded
    assert(world.getChunk(cCoords).isEmpty)

    // Clean up
    world.unload()
  }

  test("the world should allow entities to be added to and removed from a loaded chunk") {
    val provider = new FakeWorldProvider(1234)
    val world = World(provider, provider.getWorldInfo, new EntityModelLoader)
    val camera = new Camera(new CameraProjection(70, 1.6f, 0.01f, 1000f))

    val entityPosition = CylCoords(1, 2, 3)

    // Make sure the chunk is loaded
    camera.setPosition(entityPosition.toVector3d)
    world.tick(camera)
    Thread.sleep(20)
    world.tick(camera)

    var ticks = 0
    val entity = new Entity(new EntityBaseData(entityPosition), null) {
      override def tick(world: BlocksInWorld, collisionDetector: CollisionDetector): Unit = ticks += 1
    }

    world.addEntity(entity)

    assertEquals(ticks, 0)
    world.tick(camera)
    assertEquals(ticks, 1)

    world.removeEntity(entity)

    world.tick(camera)
    assertEquals(ticks, 1)

    world.addEntity(entity)

    world.tick(camera)
    assertEquals(ticks, 2)

    world.removeAllEntities()

    world.tick(camera)
    assertEquals(ticks, 2)
  }
}
