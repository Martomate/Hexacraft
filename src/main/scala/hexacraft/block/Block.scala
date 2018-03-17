package hexacraft.block

import hexacraft.HexBox
import hexacraft.world.coord.BlockRelWorld
import hexacraft.world.storage.World

object Block {
  private val maxBlocks = 256
  
  private val blocks = new Array[Block](maxBlocks)
  def byId(id: Byte): Block = blocks(id)
  
  val Air: Block= BlockAir
  val Stone     = new Block(1, "stone", "Stone")
  val Grass     = new Block(2, "grass", "Grass")
  val Dirt      = new Block(3, "dirt", "Dirt")
  val Sand      = new Block(4, "sand", "Sand")
  val Water     = new BlockFluid(5, "water", "Water")

  def init(): Unit = {
  }
}

class Block(val id: Byte, val name: String, val displayName: String) {
  Block.blocks(id) = this
  
  protected lazy val texture = new BlockTexture(name)
  def bounds(blockState: BlockState) = new HexBox(0.5f, 0, 0.5f * blockHeight(blockState))
  
  def blockTex(side: Int): Int = texture.indices(side)
  def canBeRendered: Boolean = true

  def isTransparent(blockState: BlockState, side: Int): Boolean = false

  def blockHeight(blockState: BlockState): Float = 1.0f

  final def doUpdate(coords: BlockRelWorld, world: World): Unit = onUpdated(coords, world)
  protected def onUpdated(coords: BlockRelWorld, world: World): Unit = ()
}

object BlockAir extends Block(0, "air", "Air") {
  override def canBeRendered: Boolean = false
  override def isTransparent(blockState: BlockState, side: Int): Boolean = true
}

class BlockFluid(_id: Byte, _name: String, _displayName: String) extends Block(_id, _name, _displayName) {
  override def onUpdated(coords: BlockRelWorld, world: World): Unit = {
    val bs = world.getBlock(coords).get
    var depth: Int = bs.metadata & 0x1f
    val blocks = BlockState.neighborOffsets.map(off => BlockRelWorld(coords.x + off._1, coords.y + off._2, coords.z + off._3, world.size))
    val bottomCoords = blocks.find(_.y == coords.y - 1).get
    val bottomBS = world.getBlock(bottomCoords)
    if (!bottomBS.exists(_.blockType != Block.Air)) {
      world.setBlock(bottomCoords, new BlockState(this, depth.toByte))
      depth = 0x1f
    } else if (bottomBS.exists(_.blockType == this) && bottomBS.get.metadata != 0) {
      val totalLevel = (0x1f - depth) + (0x1f - (bottomBS.get.metadata & 0x1f))
      world.setBlock(bottomCoords, new BlockState(this, (0x1f - math.min(totalLevel, 0x1f)).toByte))
      depth = 0x1f - math.max(totalLevel - 0x1f, 0)
    } else {
      blocks.filter(_.y == coords.y).map(c => (c, world.getBlock(c).getOrElse(new BlockState(Block.Air)))).foreach { case (nCoords, ns) =>
        if (ns.blockType == Block.Air) {
          val belowNeighborBlock = world.getBlock(nCoords.offset(0, -1, 0))
          val belowNeighbor = belowNeighborBlock.map(_.blockType).getOrElse(Block.Air)
          if (depth < 0x1e || (depth == 0x1e && (belowNeighbor == Block.Air || (belowNeighbor == this && belowNeighborBlock.get.metadata != 0)))) {
            world.setBlock(nCoords, new BlockState(this, 0x1e.toByte))
            depth += 1
          }
        } else if (ns.blockType == this) {
          val nsDepth: Int = ns.metadata & 0x1f
          if (depth < 0x1f) {
            if (nsDepth - 1 > depth) {
              world.setBlock(nCoords, new BlockState(this, (nsDepth - 1).toByte))
              depth += 1
            }
          }
        }
      }
    }

    if (depth >= 0x1f)
      world.removeBlock(coords)
    else if (depth != (bs.metadata & 0x1f))
      world.setBlock(coords, new BlockState(this, depth.toByte))
  }

  override def isTransparent(blockState: BlockState, side: Int): Boolean = blockState.metadata != 0

  override def blockHeight(blockState: BlockState): Float = 1f - (blockState.metadata & 0x1f) / 0x1f.toFloat
}