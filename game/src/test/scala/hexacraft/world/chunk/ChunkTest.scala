package hexacraft.world.chunk

import hexacraft.world.{CylinderSize, FakeBlocksInWorld, FakeWorldProvider, WorldGenerator}
import hexacraft.world.coord.ChunkRelWorld

import munit.FunSuite

class ChunkTest extends FunSuite {
  given CylinderSize = CylinderSize(6)

  test("the chunk should not crash") {
    val coords = ChunkRelWorld(-2, 13, 61)
    val provider = new FakeWorldProvider(1289)
    val world = FakeBlocksInWorld.empty(provider)
    val col = world.provideColumn(coords.getColumnRelWorld)
    Chunk.from(WorldGenerator(provider.worldInfo.gen).generateChunk(coords, col))
  }
}
