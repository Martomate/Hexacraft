package hexacraft.block

import hexacraft.HexBox
import hexacraft.world.coord.{BlockRelChunk, BlockRelWorld, CylCoords}
import hexacraft.world.storage.Chunk

import scala.collection.Seq

object BlockState {
  val neighborOffsets: Seq[(Int, Int, Int)] = Seq(
      ( 0, 1, 0),
      ( 0,-1, 0),
      ( 1, 0, 0),
      ( 0, 0, 1),
      (-1, 0, 1),
      (-1, 0, 0),
      ( 0, 0,-1),
      ( 1, 0,-1))

  val vertices: Seq[CylCoords] = new HexBox(0.5f, 0, 0.5f).vertices

  def getVertices(side: Int): Seq[CylCoords] = side match {
    case 0 => vertices.take(6)
    case 1 => vertices.takeRight(6).reverse
    case _ => Seq(vertices(side-2), vertices(side-2 + 6), vertices((side-1) % 6 + 6), vertices((side-1) % 6))
  }
}

case class BlockState(blockType: Block, metadata: Byte = 0)
