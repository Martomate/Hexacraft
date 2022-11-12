package com.martomate.hexacraft.world

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.{BlockFactory, BlockLoader, Blocks}
import com.martomate.hexacraft.world.chunk.Chunk
import com.martomate.hexacraft.world.coord.integer.{ChunkRelWorld, ColumnRelWorld}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ChunkCacheTest extends AnyFlatSpec with Matchers {
  given CylinderSize = new CylinderSize(8)
  given BlockLoader = new FakeBlockLoader
  given BlockFactory = new BlockFactory
  given Blocks = new Blocks

  "the cache" should "return chunks from the world" in {
    val provider = new FakeWorldProvider(1289)
    val world = FakeBlocksInWorld.empty(provider)
    val cache = new ChunkCache(world)

    // Load a chunk
    val coords = ChunkRelWorld(2, -7, 3)
    val chunk = Chunk(coords, world, provider)
    val column = world.provideColumn(ColumnRelWorld(2, 3))
    column.setChunk(chunk)

    // The cache should return the chunk
    cache.getChunk(coords) shouldBe chunk

    // Remove the chunk (and clear the cache)
    column.removeChunk(coords.getChunkRelColumn)
    cache.clearCache()

    // It should return null
    cache.getChunk(coords) shouldBe null
  }

  it should "work at the origin" in {
    val provider = new FakeWorldProvider(1289)
    val world = FakeBlocksInWorld.empty(provider)
    val cache = new ChunkCache(world)

    // Start with an empty world
    val coords = ChunkRelWorld(0, 0, 0)
    cache.getChunk(coords) shouldBe null
    cache.clearCache()

    // Load a chunk
    val chunk = Chunk(coords, world, provider)
    val column = world.provideColumn(ColumnRelWorld(0, 0))
    column.setChunk(chunk)

    // The cache should return it
    cache.getChunk(coords) shouldBe chunk

    // Remove the chunk (and clear the cache)
    column.removeChunk(coords.getChunkRelColumn)
    cache.clearCache()

    // The cache should return null
    cache.getChunk(coords) shouldBe null
  }
}
