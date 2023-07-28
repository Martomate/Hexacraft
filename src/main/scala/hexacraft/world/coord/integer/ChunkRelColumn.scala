package hexacraft.world.coord.integer

object ChunkRelColumn {
  def create(Y: Int): ChunkRelColumn = new ChunkRelColumn(Y & 0xfff)
}

case class ChunkRelColumn(value: Int) extends AnyVal { // YYY
  def Y: Int = value << 20 >> 20
}
