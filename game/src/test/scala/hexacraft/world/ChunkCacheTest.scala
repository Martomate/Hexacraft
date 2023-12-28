package hexacraft.world

import hexacraft.world.chunk.Chunk
import hexacraft.world.coord.{ChunkRelWorld, ColumnRelWorld}

import munit.FunSuite

class ChunkCacheTest extends FunSuite {
  given CylinderSize = CylinderSize(8)

  test("the cache should return chunks from the world") {
    val provider = new FakeWorldProvider(1289)
    val world = FakeBlocksInWorld.empty(provider)
    val cache = new ChunkCache(world)

    // Load a chunk
    val coords = ChunkRelWorld(2, -7, 3)
    val chunk = Chunk.fromGenerator(coords, world, WorldGenerator(provider.getWorldInfo.gen))
    val column = world.provideColumn(ColumnRelWorld(2, 3))
    column.setChunk(chunk)

    // The cache should return the chunk
    assertEquals(cache.getChunk(coords), chunk)

    // Remove the chunk (and clear the cache)
    column.removeChunk(coords.Y)
    cache.clearCache()

    // It should return null
    assertEquals(cache.getChunk(coords), null)
  }

  test("the cache should work at the origin") {
    val provider = new FakeWorldProvider(1289)
    val world = FakeBlocksInWorld.empty(provider)
    val cache = new ChunkCache(world)

    // Start with an empty world
    val coords = ChunkRelWorld(0, 0, 0)
    assertEquals(cache.getChunk(coords), null)
    cache.clearCache()

    // Load a chunk
    val chunk = Chunk.fromGenerator(coords, world, WorldGenerator(provider.getWorldInfo.gen))
    val column = world.provideColumn(ColumnRelWorld(0, 0))
    column.setChunk(chunk)

    // The cache should return it
    assertEquals(cache.getChunk(coords), chunk)

    // Remove the chunk (and clear the cache)
    column.removeChunk(coords.Y)
    cache.clearCache()

    // The cache should return null
    assertEquals(cache.getChunk(coords), null)
  }
}
