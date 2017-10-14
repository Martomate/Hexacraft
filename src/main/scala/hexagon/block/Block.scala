package hexagon.block

import hexagon.HexBox

object Block {
  private val maxBlocks = 256
  
  private val blocks = new Array[Block](maxBlocks)
  def byId(id: Byte): Block = blocks(id)
  
  val Air       = BlockAir
  val Stone     = new Block(1, "stone", "Stone")
  val Grass     = new Block(2, "grass", "Grass")
  val Dirt      = new Block(3, "dirt", "Dirt")
  val Sand      = new Block(4, "sand", "Sand")

  def init(): Unit = {
  }
}

class Block(val id: Byte, val name: String, val displayName: String) {
  Block.blocks(id) = this
  
  protected val texture = new BlockTexture(name)
  val bounds = new HexBox(0.5f, 0, 0.5f)
  
  def blockTex(side: Int): Int = texture.indices(side)
  def canBeRendered: Boolean = true
}

object BlockAir extends Block(0, "air", "Air") {
  override def canBeRendered: Boolean = false
}
