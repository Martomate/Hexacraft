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
}
