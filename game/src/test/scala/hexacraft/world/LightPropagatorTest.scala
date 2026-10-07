package hexacraft.world

import hexacraft.world.block.{Block, BlockState}
import hexacraft.world.coord.{BlockRelWorld, ChunkRelWorld}

import munit.FunSuite

class LightPropagatorTest extends FunSuite {
  given CylinderSize = CylinderSize(4)

  test("init works") {
    val chunkCoords = ChunkRelWorld(0, -1, 0) // contains y from -16 to -1
    val groundLevel = -6

    // flat ground covering the bottom part of the chunk
    val ground = for {
      x <- 0 until 16
      z <- 0 until 16
      y <- -16 to groundLevel
    } yield BlockRelWorld(x, y, z) -> BlockState(Block.Dirt)

    val world = FakeBlocksInWorld.withBlocks(ground.toMap)
    val light = LightPropagator(world, _ => ())

    light.initBrightnesses(chunkCoords)

    assertEquals(world.getColumn(chunkCoords.getColumnRelWorld).get.getHeight(1, 3), groundLevel.toShort)

    def brightnessAt(coords: BlockRelWorld): Float = {
      world.getChunk(coords.getChunkRelWorld).get.getBrightness(coords.getBlockRelChunk)
    }

    assertEqualsFloat(brightnessAt(BlockRelWorld(1, groundLevel + 1, 3)), 1.0f, 1e-6)
    assertEqualsFloat(brightnessAt(BlockRelWorld(1, groundLevel - 1, 3)), 0.0f, 1e-6)
  }
}
