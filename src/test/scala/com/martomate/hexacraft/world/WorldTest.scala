package com.martomate.hexacraft.world

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.Blocks
import com.martomate.hexacraft.world.block.state.BlockState
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

    val col = world.provideColumn(ColumnRelWorld(3, -4))
    col.coords shouldBe ColumnRelWorld(3, -4)
    world.getChunk(ChunkRelWorld(-300, 2, -95)) shouldBe None

    val cCoords = ChunkRelWorld(3, 0, -4)
    col.setChunk(Chunk(cCoords, world, provider))

    val bCoords = BlockRelWorld(3*16+5, 0, -4*16+3)
    world.getBlock(bCoords) shouldBe BlockState.Air
    world.setBlock(bCoords, BlockState(Blocks.Stone, 2))
    world.getBlock(bCoords) shouldBe BlockState(Blocks.Stone, 2)

    world.unload()
  }
}
