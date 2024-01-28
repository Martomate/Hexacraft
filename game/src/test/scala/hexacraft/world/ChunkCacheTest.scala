package hexacraft.world

import hexacraft.world.block.{Block, BlockState}
import hexacraft.world.chunk.Chunk
import hexacraft.world.coord.{BlockRelWorld, ChunkRelWorld, ColumnRelWorld}

import munit.FunSuite

class ChunkCacheTest extends FunSuite {
  given CylinderSize = CylinderSize(8)

  test("the cache should return chunks from the world") {
    val coords = ChunkRelWorld(2, -7, 3)

    val provider = new FakeWorldProvider(1289)
    val world = FakeBlocksInWorld.withBlocks(
      provider,
      Map(BlockRelWorld(9, 8, 7, coords) -> BlockState(Block.Dirt))
    )
    val chunk = world.getChunk(coords).get

    val cache = new ChunkCache(world)

    // The cache should return the chunk
    assertEquals(cache.getChunk(coords), chunk)
  }

  test("the cache should work at the origin") {
    val coords = ChunkRelWorld(0, 0, 0)

    val provider = new FakeWorldProvider(1289)
    val world = FakeBlocksInWorld.withBlocks(
      provider,
      Map(BlockRelWorld(9, 8, 7, coords) -> BlockState(Block.Dirt))
    )
    val chunk = world.getChunk(coords).get

    val cache = new ChunkCache(world)

    // The cache should return the chunk
    assertEquals(cache.getChunk(coords), chunk)
  }

  test("the cache can be cleared") {
    val coords = ChunkRelWorld(2, -7, 3)

    val provider = new FakeWorldProvider(1289)
    val world = FakeBlocksInWorld.withBlocks(
      provider,
      Map(BlockRelWorld(9, 8, 7, coords) -> BlockState(Block.Dirt))
    )
    val chunk = world.getChunk(coords).get

    val cache = new ChunkCache(world)

    // The cache should return the chunk
    assertEquals(cache.getChunk(coords), chunk)

    // Remove the chunk and clear the cache
    world.removeChunk(coords)
    cache.clearCache()

    // It should return null
    assertEquals(cache.getChunk(coords), null)
  }

}
