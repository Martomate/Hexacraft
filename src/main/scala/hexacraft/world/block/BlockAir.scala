package hexacraft.world.block

class BlockAir extends Block(0, "air", "Air") {
  override def canBeRendered: Boolean = false
  override def isTransparent(metadata: Byte, side: Int): Boolean = true
}
