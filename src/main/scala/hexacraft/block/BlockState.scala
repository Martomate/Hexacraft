package hexacraft.block

import hexacraft.world.coord.BlockRelWorld
import hexacraft.world.coord.CylCoords
import hexacraft.world.coord.BlockRelChunk
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

  val vertices: Seq[CylCoords] = {
    //val ints = Seq(1, 2, 0, 3, 5, 4)

    for {
      s <- 0 to 1
      i <- 0 until 6
    } yield {
      val v = i * Math.PI / 3
      val x = Math.cos(v).toFloat
      val z = Math.sin(v).toFloat
      new CylCoords(x * 0.5, (1 - s) * 0.5, z * 0.5, null, false)
    }
  }

  def getVertices(side: Int): Seq[CylCoords] = side match {
    case 0 => vertices.take(6)
    case 1 => vertices.takeRight(6).reverse
    case _ => Seq(vertices(side-2), vertices(side-2 + 6), vertices((side-1) % 6 + 6), vertices((side-1) % 6))
  }
}

class BlockState(val coords: BlockRelWorld, val blockType: Block, val metadata: Byte = 0) {
  def neighbor(side: Int, chunk: Chunk): Option[BlockState] = {
    val (i, j, k) = BlockState.neighborOffsets(side)
    val (i2, j2, k2) = (coords.cx + i, coords.cy + j, coords.cz + k)
    if ((i2 & ~15 | j2 & ~15 | k2 & ~15) == 0) {
      chunk.getBlock(BlockRelChunk(i2, j2, k2, coords.world))
    } else {
      chunk.world.getBlock(BlockRelWorld(chunk.coords.X * 16 + i2, chunk.coords.Y * 16 + j2, chunk.coords.Z * 16 + k2, coords.world))
    }
  }
}