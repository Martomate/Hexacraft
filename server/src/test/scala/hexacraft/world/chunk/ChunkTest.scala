package hexacraft.world.chunk

import hexacraft.world.{CylinderSize, FakeWorldProvider, WorldGenerator}
import hexacraft.world.coord.ChunkRelWorld

import munit.FunSuite

class ChunkTest extends FunSuite {
  given CylinderSize = CylinderSize(6)

  test("the chunk should not crash") {
    val coords = ChunkRelWorld(-2, 13, 61)
    val generator = WorldGenerator(new FakeWorldProvider(1289).worldInfo.gen)
    val heightMap = ChunkColumnHeightMap.fromData2D(generator.getHeightmapInterpolator(coords.getColumnRelWorld))
    Chunk.from(generator.generateChunk(coords, heightMap))
  }
}
