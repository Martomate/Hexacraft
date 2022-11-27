package com.martomate.hexacraft.world.coord

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.coord.fp.BlockCoords
import com.martomate.hexacraft.world.coord.integer.{BlockRelWorld, ChunkRelWorld}

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CoordUtilsTest extends AnyFlatSpec with Matchers {
  implicit val cylSize: CylinderSize = CylinderSize(4)

  "approximateIntCoords" should "return the input when it's integers" in {
    val f = CoordUtils.approximateIntCoords _
    f(BlockCoords(0, 0, 0)) shouldBe BlockRelWorld(0, 0, 0)
    f(BlockCoords(0, 0, cylSize.totalSize)) shouldBe BlockRelWorld(0, 0, 0)
    f(BlockCoords(20, 0, 0)) shouldBe BlockRelWorld(20, 0, 0)
    f(BlockCoords(0, 20, 0)) shouldBe BlockRelWorld(0, 20, 0)
    f(BlockCoords(0, 0, 20)) shouldBe BlockRelWorld(0, 0, 20)
    f(BlockCoords(20, 10, 0)) shouldBe BlockRelWorld(20, 10, 0)
    f(BlockCoords(0, 10, 20)) shouldBe BlockRelWorld(0, 10, 20)
    f(BlockCoords(10, 0, 20)) shouldBe BlockRelWorld(10, 0, 20)
  }

  it should "round to the closest integer" in {
    val f = CoordUtils.approximateIntCoords _
    f(BlockCoords(0.2, 0, 0)) shouldBe BlockRelWorld(0, 0, 0)
    f(BlockCoords(19.5, 0, 0)) shouldBe BlockRelWorld(20, 0, 0)
    f(BlockCoords(0, 20.4, 0)) shouldBe BlockRelWorld(0, 20, 0)
    f(BlockCoords(0, 19.5, 0)) shouldBe BlockRelWorld(0, 20, 0)
    f(BlockCoords(0, 0, 20.3)) shouldBe BlockRelWorld(0, 0, 20)
    f(BlockCoords(0, 0, (cylSize.totalSize - 1) + 0.5001)) shouldBe BlockRelWorld(0, 0, 0)
    f(BlockCoords(20.2, 9.8, 0)) shouldBe BlockRelWorld(20, 10, 0)
    f(BlockCoords(19.5, 9.5, 0)) shouldBe BlockRelWorld(20, 10, 0)
    f(BlockCoords(0, 9.5, 19.5)) shouldBe BlockRelWorld(0, 10, 20)
    f(BlockCoords(0, 9.5, 20.49)) shouldBe BlockRelWorld(0, 10, 20)
    f(BlockCoords(10.45, 0, 0.4)) shouldBe BlockRelWorld(10, 0, 0)
  }

  "approximateChunkCoords" should "return the input when it's on chunk corner" in {
    val f = CoordUtils.approximateChunkCoords _
    f(BlockCoords(0, 0, 0).toCylCoords) shouldBe ChunkRelWorld(0, 0, 0)
    f(BlockCoords(0, 0, cylSize.totalSize).toCylCoords) shouldBe ChunkRelWorld(0, 0, 0)
    f(BlockCoords(20 * 16, 0, 0).toCylCoords) shouldBe ChunkRelWorld(20, 0, 0)
    f(BlockCoords(0, 20 * 16, 0).toCylCoords) shouldBe ChunkRelWorld(0, 20, 0)
    f(BlockCoords(0, 0, 2 * 16).toCylCoords) shouldBe ChunkRelWorld(0, 0, 2)
    f(BlockCoords(20 * 16, 10 * 16, 0).toCylCoords) shouldBe ChunkRelWorld(20, 10, 0)
    f(BlockCoords(0, 10 * 16, 2 * 16).toCylCoords) shouldBe ChunkRelWorld(0, 10, 2)
    f(BlockCoords(10 * 16, 0, 2 * 16).toCylCoords) shouldBe ChunkRelWorld(10, 0, 2)
  }

  it should "floor the input to it's chunk corner" in {
    val f = CoordUtils.approximateChunkCoords _
    f(BlockCoords(0 + 5, 0 + 2, 0 + 15).toCylCoords) shouldBe ChunkRelWorld(0, 0, 0)
    f(BlockCoords(0 + 14, 0 + 1, cylSize.totalSize + 9).toCylCoords) shouldBe ChunkRelWorld(0, 0, 0)
    f(BlockCoords(20 * 16 + 15, 0 + 5, 0 + 4).toCylCoords) shouldBe ChunkRelWorld(20, 0, 0)
    f(BlockCoords(10, -20 * 16 + 3, 0).toCylCoords) shouldBe ChunkRelWorld(0, -20, 0)
    f(BlockCoords(0, 0, -2 * 16 + 1).toCylCoords) shouldBe ChunkRelWorld(0, 0, cylSize.ringSize - 2)
    f(BlockCoords(-20 * 16 + 15, 10 * 16, -1).toCylCoords) shouldBe ChunkRelWorld(-20, 10, cylSize.ringSize - 1)
    f(BlockCoords(0, 10 * 16, 2 * 16 + 4).toCylCoords) shouldBe ChunkRelWorld(0, 10, 2)
    f(BlockCoords(10 * 16 + 15, 15, 2 * 16 + 15).toCylCoords) shouldBe ChunkRelWorld(10, 0, 2)
  }
}
