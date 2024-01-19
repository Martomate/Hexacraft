package hexacraft.world.block

import hexacraft.physics.Viscosity
import hexacraft.world.HexBox

object Block {
  private val maxBlocks = 256

  private val blocks = new Array[Block](maxBlocks)
  def byId(id: Byte): Block = blocks(id)

  def register[B <: Block](block: B): B = {
    require(blocks(block.id) == null)
    blocks(block.id) = block
    block
  }

  val Air = register(new BlockAir)
  val Stone = register(new Block(1, "stone", "Stone"))
  val Grass = register(new Block(2, "grass", "Grass"))
  val Dirt = register(new Block(3, "dirt", "Dirt"))
  val Sand = register(new Block(4, "sand", "Sand") with EmittingLight)
  val Water = register(new BlockFluid(5, "water", "Water"))
  val OakLog = register(new Block(6, "log", "Oak log"))
  val OakLeaves = register(new Block(7, "leaves", "Oak leaves"))
  val Planks = register(new Block(8, "planks", "Planks"))
  val BirchLog = register(new Block(9, "log_birch", "Birch log"))
  val BirchLeaves = register(new Block(10, "leaves_birch", "Birch leaves"))
  val Tnt = register(new Block(11, "tnt", "TNT"))
}

class Block(val id: Byte, val name: String, val displayName: String) {
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

  override def equals(o: Any): Boolean = o match {
    case other: Block => id == other.id
    case _            => false
  }

  override def toString: String = displayName
}

class BlockAir extends Block(0, "air", "Air") {
  override def canBeRendered: Boolean = false
  override def isCovering(metadata: Byte, side: Int): Boolean = false
  override def isTransmissive: Boolean = true
  override def isSolid: Boolean = false
  override def viscosity: Viscosity = Viscosity.air
}

trait EmittingLight extends Block {
  override def lightEmitted: Byte = 14
}
