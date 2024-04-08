package hexacraft.world.coord

import hexacraft.world.CylinderSize

import munit.FunSuite

class CoordUtilsTest extends FunSuite {
  given cylSize: CylinderSize = CylinderSize(4)

  test("approximateIntCoords should return the input when it's integers") {
    val f = CoordUtils.approximateIntCoords
    assertEquals(f(BlockCoords(0, 0, 0)), BlockRelWorld(0, 0, 0))
    assertEquals(f(BlockCoords(0, 0, cylSize.totalSize)), BlockRelWorld(0, 0, 0))
    assertEquals(f(BlockCoords(20, 0, 0)), BlockRelWorld(20, 0, 0))
    assertEquals(f(BlockCoords(0, 20, 0)), BlockRelWorld(0, 20, 0))
    assertEquals(f(BlockCoords(0, 0, 20)), BlockRelWorld(0, 0, 20))
    assertEquals(f(BlockCoords(20, 10, 0)), BlockRelWorld(20, 10, 0))
    assertEquals(f(BlockCoords(0, 10, 20)), BlockRelWorld(0, 10, 20))
    assertEquals(f(BlockCoords(10, 0, 20)), BlockRelWorld(10, 0, 20))
  }

  test("approximateIntCoords should round to the closest integer") {
    val f = CoordUtils.approximateIntCoords
    assertEquals(f(BlockCoords(0.2, 0, 0)), BlockRelWorld(0, 0, 0))
    assertEquals(f(BlockCoords(19.5, 0, 0)), BlockRelWorld(20, 0, 0))
    assertEquals(f(BlockCoords(0, 20.4, 0)), BlockRelWorld(0, 20, 0))
    assertEquals(f(BlockCoords(0, 19.5, 0)), BlockRelWorld(0, 20, 0))
    assertEquals(f(BlockCoords(0, 0, 20.3)), BlockRelWorld(0, 0, 20))
    assertEquals(f(BlockCoords(0, 0, (cylSize.totalSize - 1) + 0.5001)), BlockRelWorld(0, 0, 0))
    assertEquals(f(BlockCoords(20.2, 9.8, 0)), BlockRelWorld(20, 10, 0))
    assertEquals(f(BlockCoords(19.5, 9.5, 0)), BlockRelWorld(20, 10, 0))
    assertEquals(f(BlockCoords(0, 9.5, 19.5)), BlockRelWorld(0, 10, 20))
    assertEquals(f(BlockCoords(0, 9.5, 20.49)), BlockRelWorld(0, 10, 20))
    assertEquals(f(BlockCoords(10.45, 0, 0.4)), BlockRelWorld(10, 0, 0))
  }

  test("approximateChunkCoords should return the input when it's on chunk corner") {
    val f = CoordUtils.approximateChunkCoords
    assertEquals(f(BlockCoords(0, 0, 0).toCylCoords), ChunkRelWorld(0, 0, 0))
    assertEquals(f(BlockCoords(0, 0, cylSize.totalSize).toCylCoords), ChunkRelWorld(0, 0, 0))
    assertEquals(f(BlockCoords(20 * 16, 0, 0).toCylCoords), ChunkRelWorld(20, 0, 0))
    assertEquals(f(BlockCoords(0, 20 * 16, 0).toCylCoords), ChunkRelWorld(0, 20, 0))
    assertEquals(f(BlockCoords(0, 0, 2 * 16).toCylCoords), ChunkRelWorld(0, 0, 2))
    assertEquals(f(BlockCoords(20 * 16, 10 * 16, 0).toCylCoords), ChunkRelWorld(20, 10, 0))
    assertEquals(f(BlockCoords(0, 10 * 16, 2 * 16).toCylCoords), ChunkRelWorld(0, 10, 2))
    assertEquals(f(BlockCoords(10 * 16, 0, 2 * 16).toCylCoords), ChunkRelWorld(10, 0, 2))
  }

  test("approximateChunkCoords should floor the input to it's chunk corner") {
    val f = CoordUtils.approximateChunkCoords
    assertEquals(f(BlockCoords(0 + 5, 0 + 2, 0 + 15).toCylCoords), ChunkRelWorld(0, 0, 0))
    assertEquals(f(BlockCoords(0 + 14, 0 + 1, cylSize.totalSize + 9).toCylCoords), ChunkRelWorld(0, 0, 0))
    assertEquals(f(BlockCoords(20 * 16 + 15, 0 + 5, 0 + 4).toCylCoords), ChunkRelWorld(20, 0, 0))
    assertEquals(f(BlockCoords(10, -20 * 16 + 3, 0).toCylCoords), ChunkRelWorld(0, -20, 0))
    assertEquals(f(BlockCoords(0, 0, -2 * 16 + 1).toCylCoords), ChunkRelWorld(0, 0, cylSize.ringSize - 2))
    assertEquals(
      f(BlockCoords(-20 * 16 + 15, 10 * 16, -1).toCylCoords),
      ChunkRelWorld(-20, 10, cylSize.ringSize - 1)
    )
    assertEquals(f(BlockCoords(0, 10 * 16, 2 * 16 + 4).toCylCoords), ChunkRelWorld(0, 10, 2))
    assertEquals(f(BlockCoords(10 * 16 + 15, 15, 2 * 16 + 15).toCylCoords), ChunkRelWorld(10, 0, 2))
  }
}
