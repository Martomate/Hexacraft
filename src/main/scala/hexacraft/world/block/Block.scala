package hexacraft.world.block

import hexacraft.physics.Viscosity

object Block {
  private val maxBlocks = 256

  private val blocks = new Array[Block](maxBlocks)
  def byId(id: Byte): Block = blocks(id)
}

class Block(val id: Byte, val name: String, val displayName: String) {
  Block.blocks(id) = this

  def bounds(metadata: Byte) = new HexBox(0.5f, 0, 0.5f * blockHeight(metadata))

  def canBeRendered: Boolean = true

  /** Is true if this block is opaque on the given side and fully covers it (e.g. a slab would only cover one side) */
  def isCovering(metadata: Byte, side: Int): Boolean = true

  /** Is true if light can pass through this block (e.g. water, tinted glass, but NOT slabs) */
  def isTransmissive: Boolean = false

  def isSolid: Boolean = true

  /** Only applicable for non-solids */
  def viscosity: Viscosity = Viscosity.fromSI(0)

  def lightEmitted: Byte = 0

  def blockHeight(metadata: Byte): Float = 1.0f

  val behaviour: Option[BlockBehaviour] = None

  override def equals(o: Any): Boolean = o match
    case other: Block => id == other.id
    case _            => false

  override def toString: String = displayName
}
