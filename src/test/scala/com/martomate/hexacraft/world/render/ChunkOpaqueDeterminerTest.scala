package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.Blocks
import com.martomate.hexacraft.world.block.state.BlockState
import com.martomate.hexacraft.world.chunk.storage.{ChunkStorage, DenseChunkStorage, SparseChunkStorage}
import com.martomate.hexacraft.world.coord.integer.{BlockRelChunk, ChunkRelWorld}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

abstract class ChunkOpaqueDeterminerTest extends AnyFlatSpec with Matchers with MockFactory {
  def make(chunk: ChunkStorage): ChunkOpaqueDeterminer
  implicit val cylSize: CylinderSize = new CylinderSize(4)
  val coords00 = ChunkRelWorld(0, 0, 0)

  "canGetToSide" should "return true for empty chunk" in {
    val det = make(new SparseChunkStorage(coords00))

    testAllSides(det)((_, _) => true)
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

    testAllSides(det)((_, _) => false)
  }

  it should "return false for a blocked off side" in {
    val chunk = new DenseChunkStorage(coords00)
    val det = make(chunk)

    val block = new BlockState(Blocks.Dirt)
    for {
      x <- 0 until 16
      z <- 0 until 16
    } chunk.setBlock(BlockRelChunk(x, 15, z), block)

    // it's not defined what (0, 0) should map to. false for now

    testAllSides(det)((f, t) => !(f == 0 || t == 0))
  }

  it should "return false for side on opposite side of a wall" in {
    val chunk = new DenseChunkStorage(coords00)
    val det = make(chunk)

    val block = new BlockState(Blocks.Dirt)
    for {
      x <- 0 until 16
      z <- 0 until 16
    } chunk.setBlock(BlockRelChunk(x, 5, z), block)

    testAllSides(det)((f, t) => !(f == 0 && t == 1 || t == 0 && f == 1))
  }

  "invalidate" should "refresh the side data" in {
    val chunk = new DenseChunkStorage(coords00)
    val det = make(chunk)

    testAllSides(det)((_, _) => true)

    val block = new BlockState(Blocks.Dirt)
    for {
      x <- 0 until 16
      y <- 0 until 16
      z <- 0 until 16
    } chunk.setBlock(BlockRelChunk(x, y, z), block)

    testAllSides(det)((_, _) => true)

    det.invalidate()

    testAllSides(det)((_, _) => false)
  }

  def testAllSides(det: ChunkOpaqueDeterminer)(answer: (Int, Int) => Boolean): Unit = for {
    f <- 0 until 8
    t <- 0 until 8
  } (f, t, det.canGetToSide(f, t)) shouldBe ((f, t, answer(f, t)))
}
