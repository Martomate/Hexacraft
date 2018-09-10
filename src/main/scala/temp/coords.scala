package temp

object coords {
  class BlockInChunkCoordsImpl(val cx: Byte, val cy: Byte, val cz: Byte) extends BlockInChunkCoords

  object BlockInChunkCoords {
    def apply(cx: Byte, cy: Byte, cz: Byte): BlockInChunkCoords = new BlockInChunkCoordsImpl(cx, cy, cz)
  }
  trait BlockInChunkCoords {
    def cx: Byte
    def cy: Byte
    def cz: Byte
  }

  class ChunkInColumnCoordsImpl(val Y: Short) extends ChunkInColumnCoords

  trait ChunkInColumnCoords {
    def Y: Short
  }
  trait ColumnInWorldCoords {
    def X: Int
    def Z: Int
  }

  trait BlockInColumnCoords extends BlockInChunkCoords with ChunkInColumnCoords
  trait ChunkInWorldCoords extends ChunkInColumnCoords with ColumnInWorldCoords

  trait BlockInWorldCoords extends BlockInColumnCoords with ChunkInWorldCoords with
    BlockInChunkCoords with ChunkInColumnCoords with ColumnInWorldCoords

  def test(c: BlockInColumnCoords): Unit = {
    println(c.getClass)
  }

  def test2(c: BlockInWorldCoords): Unit = {
    test(c)
  }

  def main(args: Array[String]): Unit = {
    test2(new BlockInWorldCoords {
      override def Y: Short = ???

      override def cx: Byte = ???

      override def cy: Byte = ???

      override def cz: Byte = ???

      override def X: Int = ???

      override def Z: Int = ???
    })
  }
}
