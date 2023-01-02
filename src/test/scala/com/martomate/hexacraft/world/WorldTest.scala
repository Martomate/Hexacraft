package com.martomate.hexacraft.world

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.{BlockFactory, BlockLoader, Blocks, BlockState}
import com.martomate.hexacraft.world.camera.{Camera, CameraProjection}
import com.martomate.hexacraft.world.chunk.Chunk
import com.martomate.hexacraft.world.coord.fp.{BlockCoords, CylCoords}
import com.martomate.hexacraft.world.coord.integer.{BlockRelChunk, BlockRelWorld, ChunkRelWorld, ColumnRelWorld}
import com.martomate.hexacraft.world.entity.{Entity, EntityBaseData, EntityModel, EntityModelLoader}
import com.martomate.hexacraft.world.entity.base.BasicEntityModel
import com.martomate.hexacraft.world.entity.player.{PlayerEntity, PlayerEntityModel, PlayerFactory}

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class WorldTest extends AnyFlatSpec with Matchers {
  given CylinderSize = CylinderSize(8)
  given BlockLoader = new FakeBlockLoader
  given BlockFactory = new BlockFactory
  given Blocks: Blocks = new Blocks
  given EntityModelLoader = new EntityModelLoader

  "the world" should "not crash" in {
    val provider = new FakeWorldProvider(1234)
    val world = World(provider)
    val camera = new Camera(new CameraProjection(70, 1.6f, 0.01f, 1000f))

    world.tick(camera)

    val cCoords = ChunkRelWorld(3, 7, -4)

    // Set a chunk in the world
    world.getChunk(cCoords) shouldBe None
    val chunk = Chunk(cCoords, world, provider)
    world.setChunk(chunk)
    world.getChunk(cCoords) shouldBe Some(chunk)

    // Set a block in the chunk
    val bCoords = BlockRelWorld(5, 1, 3, cCoords)
    world.getBlock(bCoords) shouldBe BlockState.Air
    world.setBlock(bCoords, BlockState(Blocks.Stone, 2))
    world.getBlock(bCoords) shouldBe BlockState(Blocks.Stone, 2)

    // Clean up
    world.unload()
  }

  it should "decorate new chunks" in {
    val provider = new FakeWorldProvider(1234)
    val world = World(provider)

    val chunkCoords = ChunkRelWorld(3, -1, -4) // this chunk contains the ground

    // Set a chunk in the world
    val chunk = Chunk(chunkCoords, world, provider)
    world.setChunk(chunk)

    // The planner should have decorated the chunk
    chunk.isDecorated shouldBe true

    // There should be a tree in the chunk
    chunk.blocks.allBlocks.exists(s => s.block.blockType == Blocks.Log) shouldBe true

    // Clean up
    world.unload()
  }

  it should "load chunks close to the camera" in {
    val provider = new FakeWorldProvider(1234)
    val world = World(provider)
    val camera = new Camera(new CameraProjection(70, 1.6f, 0.01f, 1000f))

    val cCoords = ChunkRelWorld(3, 7, -4)
    camera.setPosition(BlockCoords(BlockRelWorld(8, 8, 8, cCoords)).toCylCoords.toVector3d)

    // The chunk should be unloaded from the beginning
    world.getChunk(cCoords).isDefined shouldBe false

    // Run the game a bit
    world.tick(camera)
    Thread.sleep(10)
    world.tick(camera)

    // The chunk should be loaded
    world.getChunk(cCoords).isDefined shouldBe true

    // Clean up
    world.unload()
  }

  it should "unload chunks far from the camera" in {
    val provider = new FakeWorldProvider(1234)
    val world = World(provider)
    val camera = new Camera(new CameraProjection(70, 1.6f, 0.01f, 1000f))

    val cCoords = ChunkRelWorld(3, 7, -4)
    camera.setPosition(BlockCoords(BlockRelWorld(8, 8, 8, cCoords)).toCylCoords.toVector3d)

    // Run the game a bit
    world.tick(camera)
    Thread.sleep(10)
    world.tick(camera)

    // The chunk should be loaded
    world.getChunk(cCoords).isDefined shouldBe true

    // Move far away
    camera.setPosition(BlockCoords(BlockRelWorld(8, 8, 8, cCoords.offset(100, 0, 0))).toCylCoords.toVector3d)

    // Run the game a bit
    world.tick(camera)
    Thread.sleep(10)
    world.tick(camera)

    // The chunk should be unloaded
    world.getChunk(cCoords).isDefined shouldBe false

    // Clean up
    world.unload()
  }

  it should "allow entities to be added to and removed from a loaded chunk" in {
    val provider = new FakeWorldProvider(1234)
    val world = World(provider)
    val camera = new Camera(new CameraProjection(70, 1.6f, 0.01f, 1000f))

    val entityPosition = CylCoords(1, 2, 3)

    // Make sure the chunk is loaded
    camera.setPosition(entityPosition.toVector3d)
    world.tick(camera)
    Thread.sleep(10)
    world.tick(camera)

    var ticks = 0
    val entity = new Entity(new EntityBaseData(entityPosition), null) {
      override def tick(world: BlocksInWorld, collisionDetector: CollisionDetector): Unit = ticks += 1
    }

    world.addEntity(entity)

    ticks shouldBe 0
    world.tick(camera)
    ticks shouldBe 1

    world.removeEntity(entity)

    world.tick(camera)
    ticks shouldBe 1

    world.addEntity(entity)

    world.tick(camera)
    ticks shouldBe 2

    world.removeAllEntities()

    world.tick(camera)
    ticks shouldBe 2
  }
}
