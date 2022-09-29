package com.martomate.hexacraft.world

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.{BlockState, Blocks}
import com.martomate.hexacraft.world.camera.{Camera, CameraProjection}
import com.martomate.hexacraft.world.chunk.Chunk
import com.martomate.hexacraft.world.coord.integer.{BlockRelWorld, ChunkRelWorld, ColumnRelWorld}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class WorldTest extends AnyFlatSpec with Matchers {
  implicit val cylSize: CylinderSize = new CylinderSize(8)

  "the world" should "not crash" in {
    val provider = new FakeWorldProvider(1234)
    val world = new World(provider)
    val camera = new Camera(new CameraProjection(70, 1.6f, 0.01f, 1000f))

    world.tick(camera)

    val cCoords = ChunkRelWorld(3, 7, -4)

    val col = world.provideColumn(cCoords.getColumnRelWorld)
    col.coords shouldBe cCoords.getColumnRelWorld

    // Set a chunk in the world
    world.getChunk(cCoords) shouldBe None
    val chunk = Chunk(cCoords, world, provider)
    col.setChunk(chunk)
    world.getChunk(cCoords) shouldBe Some(chunk)

    // Set a block in the chunk
    val bCoords = BlockRelWorld(5, 1, 3, cCoords)
    world.getBlock(bCoords) shouldBe BlockState.Air
    world.setBlock(bCoords, BlockState(Blocks.Stone, 2))
    world.getBlock(bCoords) shouldBe BlockState(Blocks.Stone, 2)

    world.unload()
  }
}
