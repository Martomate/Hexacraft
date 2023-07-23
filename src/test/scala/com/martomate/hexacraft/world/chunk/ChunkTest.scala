package com.martomate.hexacraft.world.chunk

import com.martomate.hexacraft.world.{CylinderSize, FakeBlockLoader, FakeBlocksInWorld, FakeWorldProvider}
import com.martomate.hexacraft.world.block.{BlockLoader, Blocks}
import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld
import com.martomate.hexacraft.world.entity.EntityModelLoader

import munit.FunSuite

class ChunkTest extends FunSuite {
  given CylinderSize = CylinderSize(6)
  given BlockLoader = new FakeBlockLoader
  given Blocks = new Blocks
  given EntityModelLoader = new EntityModelLoader

  test("the chunk should not crash") {
    val coords = ChunkRelWorld(-2, 13, 61)
    val provider = new FakeWorldProvider(1289)
    val world = FakeBlocksInWorld.empty(provider)
    val chunk = Chunk(coords, world, provider)
    assertEquals(chunk.coords, coords)
  }
}
