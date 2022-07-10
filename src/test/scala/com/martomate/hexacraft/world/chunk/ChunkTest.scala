package com.martomate.hexacraft.world.chunk

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld
import com.martomate.hexacraft.world.{FakeBlocksInWorld, FakeWorldProvider}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ChunkTest extends AnyFlatSpec with Matchers {
  implicit val cylSize: CylinderSize = new CylinderSize(6)

  "the chunk" should "not crash" in {
    val coords = ChunkRelWorld(-2, 13, 61)
    val provider = new FakeWorldProvider(1289)
    val world = FakeBlocksInWorld.empty(provider)
    val chunk = Chunk(coords, world, provider)
    chunk.coords shouldBe coords
  }
}
