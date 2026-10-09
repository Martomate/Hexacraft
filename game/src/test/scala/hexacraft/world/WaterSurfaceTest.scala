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

  test("heightNear finds the surface when the position is in a partially filled block but above the water") {
    val world = FakeBlocksInWorld.withBlocks(waterColumn(10, 12, top = halfWater))

    assertEqualsDouble(WaterSurface.heightNear(eyeAt(12.8), world).get, 6.25, 1e-9)
  }

  test("heightNear finds the surface when the position is slightly above the water") {
    val world = FakeBlocksInWorld.withBlocks(waterColumn(10, 12))

    assertEqualsDouble(WaterSurface.heightNear(eyeAt(13.5), world).get, 6.5, 1e-9)
    assertEqualsDouble(WaterSurface.heightNear(eyeAt(14.9), world).get, 6.5, 1e-9)
  }

  test("heightNear returns None when the position is far above the water") {
    val world = FakeBlocksInWorld.withBlocks(waterColumn(10, 12))

    assertEquals(WaterSurface.heightNear(eyeAt(15.1), world), None)
    assertEquals(WaterSurface.heightNear(eyeAt(20), world), None)
  }

  test("heightNear returns None when there is something between the position and the water") {
    val world = FakeBlocksInWorld.withBlocks(waterColumn(10, 12) + (BlockRelWorld(2, 13, 3) -> BlockState(Block.Stone)))

    assertEquals(WaterSurface.heightNear(eyeAt(14.5), world), None)
  }

  test("heightNear returns None when there is no water") {
    val world = FakeBlocksInWorld.withBlocks(Map(BlockRelWorld(2, 10, 3) -> BlockState(Block.Dirt)))

    assertEquals(WaterSurface.heightNear(eyeAt(11.5), world), None)
  }
}
