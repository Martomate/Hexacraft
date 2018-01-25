package hexagon.block

import hexagon.world.coord.BlockRelWorld
import hexagon.world.coord.CylCoord
import hexagon.world.coord.BlockRelChunk
import hexagon.world.storage.Chunk
import scala.collection.Seq

object BlockState {
  val neighborOffsets = Seq(
      ( 0, 1, 0),
      ( 0,-1, 0),
      ( 1, 0, 0),
      ( 0, 0, 1),
      (-1, 0, 1),
      (-1, 0, 0),
      ( 0, 0,-1),
      ( 1, 0,-1))

  val vertices: Seq[CylCoord] = {
    //val ints = Seq(1, 2, 0, 3, 5, 4)

    for {
      s <- 0 to 1
      i <- 0 until 6
    } yield {
      val v = i * Math.PI / 3
      val x = Math.cos(v).toFloat
      val z = Math.sin(v).toFloat
      new CylCoord(x * 0.5, (1 - s) * 0.5, z * 0.5, null, false)
    }
  }

  def getVertices(side: Int): Seq[CylCoord] = side match {
    case 0 => vertices.take(6)
    case 1 => vertices.takeRight(6).reverse
    case _ => Seq(vertices(side-2), vertices(side-2 + 6), vertices((side-1) % 6 + 6), vertices((side-1) % 6))
  }
}

class BlockState(val coord: BlockRelWorld, val blockType: Block) {
  def neighbor(side: Int, chunk: Chunk): Option[BlockState] = {
    val (i, j, k) = BlockState.neighborOffsets(side)
    val (i2, j2, k2) = (coord.cx + i, coord.cy + j, coord.cz + k)
    if ((i2 & ~15 | j2 & ~15 | k2 & ~15) == 0) {
      chunk.getBlock(BlockRelChunk(i2, j2, k2, coord.world))
    } else {
      chunk.world.getBlock(BlockRelWorld(chunk.coords.X * 16 + i2, chunk.coords.Y * 16 + j2, chunk.coords.Z * 16 + k2, coord.world))
    }
  }
}
