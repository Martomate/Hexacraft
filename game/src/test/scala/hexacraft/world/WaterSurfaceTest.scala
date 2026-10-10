package hexacraft.world

import hexacraft.world.block.{Block, BlockState}
import hexacraft.world.coord.{BlockCoords, BlockRelWorld}

import munit.FunSuite

class WaterSurfaceTest extends FunSuite {
  given CylinderSize = CylinderSize(8)

  private val water = BlockState(Block.Water)
  private val halfWater = BlockState(Block.Water, 16) // blockHeight = 0.5

  private def waterColumn(fromY: Int, toY: Int, top: BlockState = water): Map[BlockRelWorld, BlockState] = {
    (fromY to toY).map(y => BlockRelWorld(2, y, 3) -> (if y == toY then top else water)).toMap
  }

  private def eyeAt(y: Double) = BlockCoords(2, y, 3).toCylCoords

  test("heightNear returns the top of the water column when the position is under water") {
    val world = FakeBlocksInWorld.withBlocks(waterColumn(10, 12))

    assertEqualsDouble(WaterSurface.heightNear(eyeAt(10.5), world).get, 6.5, 1e-9)
  }

  test("heightNear takes the fluid level of the top block into account") {
    val world = FakeBlocksInWorld.withBlocks(waterColumn(10, 12, top = halfWater))

    assertEqualsDouble(WaterSurface.heightNear(eyeAt(10.5), world).get, 6.25, 1e-9)
  }

  test("heightNear finds the surface when the position is in a partially filled block but right above the water") {
    val world = FakeBlocksInWorld.withBlocks(waterColumn(10, 12, top = halfWater))

    assertEqualsDouble(WaterSurface.heightNear(eyeAt(12.6), world).get, 6.25, 1e-9)
    assertEquals(WaterSurface.heightNear(eyeAt(12.8), world), None)
  }

  test("heightNear finds the surface when the position is right above the water") {
    val world = FakeBlocksInWorld.withBlocks(waterColumn(10, 12))

    assertEqualsDouble(WaterSurface.heightNear(eyeAt(13.01), world).get, 6.5, 1e-9)
    assertEqualsDouble(WaterSurface.heightNear(eyeAt(13.19), world).get, 6.5, 1e-9)
  }

  test("heightNear returns None when the position is further above the water") {
    val world = FakeBlocksInWorld.withBlocks(waterColumn(10, 12))

    assertEquals(WaterSurface.heightNear(eyeAt(13.5), world), None)
    assertEquals(WaterSurface.heightNear(eyeAt(20), world), None)
  }

  test("heightNear finds the surface next to a block that is above the position") {
    val underLedge = waterColumn(10, 12) + (BlockRelWorld(2, 13, 3) -> BlockState(Block.Stone))
    val openWater = (10 to 14).map(y => BlockRelWorld(3, y, 3) -> water).toMap
    val world = FakeBlocksInWorld.withBlocks(underLedge ++ openWater)

    assertEqualsDouble(WaterSurface.heightNear(eyeAt(10.5), world).get, 7.5, 1e-9)
  }

  test("heightNear finds the surface further away when swimming under a wide overhang") {
    val stone = BlockState(Block.Stone)
    // water from y = 10 to 12 below a stone ceiling at y = 13, for x = 0 to 5, and open water up to y = 14 at x = 6
    val underOverhang = for x <- 0 to 5; y <- 10 to 13
    yield BlockRelWorld(x, y, 3) -> (if y == 13 then stone else water)
    val openWater = for y <- 10 to 14 yield BlockRelWorld(6, y, 3) -> water
    val world = FakeBlocksInWorld.withBlocks((underOverhang ++ openWater).toMap)

    assertEqualsDouble(WaterSurface.heightNear(BlockCoords(0, 10.5, 3).toCylCoords, world).get, 7.5, 1e-9)
  }

  test("heightNear uses the highest water when there is no air above any of it") {
    val world = FakeBlocksInWorld.withBlocks(waterColumn(10, 12) + (BlockRelWorld(2, 13, 3) -> BlockState(Block.Stone)))

    // the chunk around is otherwise air, so close it in to make it a flooded cave
    val walls = for
      off <- Seq((1, 0), (0, 1), (-1, 1), (-1, 0), (0, -1), (1, -1))
      y <- 10 to 12
    yield BlockRelWorld(2 + off._1, y, 3 + off._2) -> BlockState(Block.Stone)
    for (c, b) <- walls do world.addBlock(c, b)

    assertEqualsDouble(WaterSurface.heightNear(eyeAt(10.5), world).get, 6.5, 1e-9)
  }

  test("heightNear returns None when there is no water") {
    val world = FakeBlocksInWorld.withBlocks(Map(BlockRelWorld(2, 10, 3) -> BlockState(Block.Dirt)))

    assertEquals(WaterSurface.heightNear(eyeAt(11.5), world), None)
  }
}
