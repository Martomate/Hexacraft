package com.martomate.hexacraft.world.block

import com.martomate.hexacraft.world.block.{Block, Blocks, HexBox}
import com.martomate.hexacraft.world.coord.fp.CylCoords

object BlockState {
  val Air: BlockState = new BlockState(Blocks.Air)

  val boundingBox: HexBox = new HexBox(0.5f, 0, 0.5f)
  val vertices: Seq[CylCoords] = boundingBox.vertices

  def getVertices(side: Int): Seq[CylCoords] = side match {
    case 0 => vertices.take(6)
    case 1 => vertices.takeRight(6).reverse
    case _ =>
      Seq(
        vertices(side - 2),
        vertices(side - 2 + 6),
        vertices((side - 1) % 6 + 6),
        vertices((side - 1) % 6)
      )
  }
}

case class BlockState(blockType: Block, metadata: Byte = 0)
