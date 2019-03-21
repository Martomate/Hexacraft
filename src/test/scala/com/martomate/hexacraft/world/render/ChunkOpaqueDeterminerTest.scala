package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.Blocks
import com.martomate.hexacraft.world.block.state.BlockState
import com.martomate.hexacraft.world.coord.integer.{BlockRelChunk, ChunkRelWorld}
import com.martomate.hexacraft.world.storage.{ChunkStorage, DenseChunkStorage, SparseChunkStorage}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers}

abstract class ChunkOpaqueDeterminerTest extends FlatSpec with Matchers with MockFactory {
  def make(chunk: ChunkStorage): ChunkOpaqueDeterminer
  implicit val cylSize: CylinderSize = new CylinderSize(4)
  val coords00 = ChunkRelWorld(0, 0, 0)

  "canGetToSide" should "return true for empty chunk" in {
    val det = make(new SparseChunkStorage(coords00))

    for {
      f <- 0 until 8
      t <- 0 until 8
    } det.canGetToSide(f, t) shouldBe true
  }

  it should "return false for full chunk" in {
    val chunk = new DenseChunkStorage(coords00)
    val det = make(chunk)

    val block = new BlockState(Blocks.Dirt)
    for {
      x <- 0 until 16
      y <- 0 until 16
      z <- 0 until 16
    } chunk.setBlock(BlockRelChunk(x, y, z), block)

    for {
      f <- 0 until 8
      t <- 0 until 8
    } det.canGetToSide(f, t) shouldBe false
  }

  it should "return false for a blocked off side" in {
    val chunk = new DenseChunkStorage(coords00)
    val det = make(chunk)

    val block = new BlockState(Blocks.Dirt)
    for {
      x <- 0 until 16
      z <- 0 until 16
    } chunk.setBlock(BlockRelChunk(x, 0, z), block)

    det.canGetToSide(0, 1) shouldBe false
    det.canGetToSide(0, 5) shouldBe false
    det.canGetToSide(0, 2) shouldBe false
    det.canGetToSide(2, 0) shouldBe false
    det.canGetToSide(3, 0) shouldBe false

    det.canGetToSide(0, 0) shouldBe true
    det.canGetToSide(2, 1) shouldBe true
    det.canGetToSide(5, 6) shouldBe true
    det.canGetToSide(4, 1) shouldBe true

    for {
      f <- 0 until 8
      t <- 0 until 8
    } (f, t, det.canGetToSide(f, t)) shouldBe (f, t, !(f == 0 ^ t == 0))
  }
}
