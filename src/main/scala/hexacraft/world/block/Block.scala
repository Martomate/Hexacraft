package hexacraft.world.block

import hexacraft.world.CylinderSize
import hexacraft.world.coord.integer.BlockRelWorld

object Block {
  private val maxBlocks = 256

  private val blocks = new Array[Block](maxBlocks)
  def byId(id: Byte): Block = blocks(id)
}

class Block(val id: Byte, val name: String, val displayName: String) {
  Block.blocks(id) = this

  def bounds(metadata: Byte) = new HexBox(0.5f, 0, 0.5f * blockHeight(metadata))

  def canBeRendered: Boolean = true

  def isTransparent(metadata: Byte, side: Int): Boolean = false

  def lightEmitted: Byte = 0

  def blockHeight(metadata: Byte): Float = 1.0f

  val behaviour: Option[BlockBehaviour] = None

  override def equals(o: Any): Boolean = o match {
    case other: Block =>
      id == other.id
    case _ => false
  }

  override def toString: String = displayName
}
