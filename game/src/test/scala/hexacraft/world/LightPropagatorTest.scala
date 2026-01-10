package hexacraft.world

import hexacraft.world.chunk.Chunk
import hexacraft.world.coord.{BlockRelWorld, ColumnRelWorld}

import munit.FunSuite

class LightPropagatorTest extends FunSuite {
  given CylinderSize = CylinderSize(4)

  // TODO: reduce the amount of setup code needed for this kind of test
  test("init works") {
    val seed = 123L
    val provider = FakeWorldProvider(seed)
    val world = FakeBlocksInWorld.empty(provider)
    val light = LightPropagator(world, _ => ())

    val colCoords = ColumnRelWorld(0, 0)
    val column = world.provideColumn(colCoords)
    val chunkCoords = BlockRelWorld(0, column.terrainHeight.getHeight(0, 0), 0).getChunkRelWorld

    world.setChunk(
      chunkCoords,
      Chunk.fromGenerator(chunkCoords, column, WorldGenerator(WorldGenSettings.fromSeed(seed)))
    )

    light.initBrightnesses(chunkCoords)

    assertEquals(column.terrainHeight.getHeight(1, 3), -5.toShort)

    {
      val blockCoords = BlockRelWorld(1, -5, 3)
      val chunk = world.getChunk(blockCoords.getChunkRelWorld).get
      val brightness = chunk.getBrightness(blockCoords.getBlockRelChunk)
      assertEqualsFloat(brightness, 1.0f, 1e-6)
    }
    {
      val blockCoords = BlockRelWorld(1, -6, 3)
      val chunk = world.getChunk(blockCoords.getChunkRelWorld).get
      val brightness = chunk.getBrightness(blockCoords.getBlockRelChunk)
      assertEqualsFloat(brightness, 0.0f, 1e-6)
    }
  }
}
