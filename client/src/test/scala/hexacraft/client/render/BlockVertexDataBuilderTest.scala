package hexacraft.client.render

import hexacraft.world.{CylinderSize, FakeBlocksInWorld, FakeWorldProvider}
import hexacraft.world.block.{Block, BlockState}
import hexacraft.world.coord.*

import munit.FunSuite

class BlockVertexDataBuilderTest extends FunSuite {
  given CylinderSize = CylinderSize(8)

  private def testWaterHeight(spots: (Offset, BlockState)*)(expectedHeights: ((Int, Int), Int)*): Unit = {
    val centerCoords = BlockRelWorld(0, 0, 0)

    // ensure world has no blocks around the relevant blocks
    val airSpots = spots
      .map(_._1)
      .distinct
      .flatMap(c => NeighborOffsets.all.toSeq.map(_ + c))
      .map(_ -> BlockState(Block.Air))

    // place air blocks, then other blocks
    val allBlocks = Seq(airSpots, spots.toSeq).flatten.map { case (offset, block) =>
      centerCoords.offset(offset) -> block
    }.toMap

    // TODO: make it possible to create a fake world with only air so we don't need the air above
    val world = FakeBlocksInWorld.withBlocks(FakeWorldProvider(1234), allBlocks)

    val blockTextureIndices = Map("water" -> IndexedSeq.fill(8)(0))
    val data = BlockVertexDataBuilder.fromChunk(centerCoords.getChunkRelWorld, world, true, blockTextureIndices)

    for ((x, z), height) <- expectedHeights do {
      val relevantVertices = data.calculateVertices(0).filter(v => v.position.x == x && v.position.z == z)
      assertEquals(relevantVertices.map(_.position.y).toSeq.distinct, Seq(height))
    }
  }

  private def testBrightness(blocks: Offset*)(brightnesses: (Offset, Byte)*)(
      expectedBrightnesses: ((Int, Int, Int), Float)*
  ): Unit = {
    val centerCoords = BlockRelWorld(0, 0, 0)

    val allBlocks = blocks.map { offset =>
      centerCoords.offset(offset) -> BlockState(Block.Stone)
    }.toMap

    val world = FakeBlocksInWorld.withBlocks(FakeWorldProvider(1234), allBlocks)

    for (offset, brightness) <- brightnesses do {
      val c = centerCoords.offset(offset)
      world.getChunk(c.getChunkRelWorld).get.setSunlight(c.getBlockRelChunk, brightness)
    }

    val blockTextureIndices = Map("stone" -> IndexedSeq.fill(8)(0))
    val data = BlockVertexDataBuilder.fromChunk(centerCoords.getChunkRelWorld, world, false, blockTextureIndices)

    for ((x, y, z), brightness) <- expectedBrightnesses do {
      val relevantVertices = data.calculateVertices(0).filter { v =>
        v.position.x == x && v.position.y == y && v.position.z == z
      }
      val values = relevantVertices.map(_.brightness).toSeq.distinct

      assertEquals(values.size, 1)
      assertEqualsFloat(values.head, brightness, 1e-6, clue = (x, y, z))
    }
  }

  private def waterHeight(metadata: Byte): Int = (32 - metadata) * 6

  test("water with air around it") {
    testWaterHeight(
      Offset(0, 0, 0) -> BlockState(Block.Water)
    )(
      (0, 0) -> waterHeight(0),
      (1, 1) -> waterHeight(0),
      (-1, 1) -> waterHeight(0)
    )

    testWaterHeight(
      Offset(0, 0, 0) -> BlockState(Block.Water, 7)
    )(
      (0, 0) -> waterHeight(7),
      (1, 1) -> waterHeight(7),
      (-1, 1) -> waterHeight(7)
    )
  }

  test("water with water next to it") {
    testWaterHeight(
      Offset(0, 0, 0) -> BlockState(Block.Water, 7),
      Offset(0, 0, 1) -> BlockState(Block.Water, 4)
    )(
      (0, 0) -> waterHeight(7),
      (1, 1) -> (waterHeight(7) + waterHeight(4)) / 2,
      (-1, 1) -> (waterHeight(7) + waterHeight(4)) / 2
    )
  }

  test("water with water and stone next to it") {
    testWaterHeight(
      Offset(0, 0, 0) -> BlockState(Block.Water, 7),
      Offset(0, 0, 1) -> BlockState(Block.Water, 4),
      Offset(1, 0, 0) -> BlockState(Block.Stone)
    )(
      (0, 0) -> waterHeight(7),
      (1, 1) -> (waterHeight(7) + waterHeight(4)) / 2,
      (-1, 1) -> (waterHeight(7) + waterHeight(4)) / 2
    )
  }

  test("water with water next to it twice") {
    testWaterHeight(
      Offset(0, 0, 0) -> BlockState(Block.Water, 7),
      Offset(0, 0, 1) -> BlockState(Block.Water, 4),
      Offset(1, 0, 0) -> BlockState(Block.Water, 5)
    )(
      (0, 0) -> waterHeight(7),
      (1, 1) -> (waterHeight(7) + waterHeight(4) + waterHeight(5)) / 3,
      (-1, 1) -> (waterHeight(7) + waterHeight(4)) / 2
    )
  }

  test("brightness gradient on flat ground") {
    testBrightness(
      Offset(0, 0, 0),
      Offset(0, 0, 1),
      Offset(1, 0, 0)
    )(
      Offset(0, 1, 0) -> 10,
      Offset(0, 1, 1) -> 9,
      Offset(1, 1, 0) -> 9
    )(
      (0, 32 * 6, 0) -> 10.0f / 15.0f,
      (1, 32 * 6, 1) -> (10.0f + 9.0f + 9.0f) / 3.0f / 15.0f
    )
  }

  test("brightness gradient on flat ground with obstacle") {
    val ambientOcclusionFactor = (2 - 1).toFloat / (3 - 1) * 0.2f + 0.8f

    testBrightness(
      Offset(0, 0, 0),
      Offset(0, 0, 1),
      Offset(1, 1, 0)
    )(
      Offset(0, 1, 0) -> 10,
      Offset(0, 1, 1) -> 9
    )(
      (1, 32 * 6, 1) -> (10.0f + 9.0f) / 2.0f / 15.0f * ambientOcclusionFactor
    )
  }
}
