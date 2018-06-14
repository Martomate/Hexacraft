package hexacraft.block

import hexacraft.HexBox
import hexacraft.world.coord.BlockRelWorld
import hexacraft.world.storage.{BlockSetAndGet, World}

object Block {
  private val maxBlocks = 256
  
  private val blocks = new Array[Block](maxBlocks)
  def byId(id: Byte): Block = blocks(id)
  
  val Air: Block= BlockAir
  val Stone     = new Block(1, "stone", "Stone")
  val Grass     = new Block(2, "grass", "Grass")
  val Dirt      = new Block(3, "dirt", "Dirt")
  val Sand      = new Block(4, "sand", "Sand") with EmittingLight
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

  def lightEmitted: Byte = 0

  def blockHeight(blockState: BlockState): Float = 1.0f

  final def doUpdate(coords: BlockRelWorld, world: BlockSetAndGet): Unit = onUpdated(coords, world)
  protected def onUpdated(coords: BlockRelWorld, world: BlockSetAndGet): Unit = ()
}

object BlockAir extends Block(0, "air", "Air") {
  val State: BlockState = BlockState(this)

  override def canBeRendered: Boolean = false
  override def isTransparent(blockState: BlockState, side: Int): Boolean = true
}

trait EmittingLight extends Block {
  override def lightEmitted: Byte = 14
}

class BlockFluid(_id: Byte, _name: String, _displayName: String) extends Block(_id, _name, _displayName) {
  private val fluidLevelMask = 0x1f

  override def onUpdated(coords: BlockRelWorld, world: BlockSetAndGet): Unit = {
    val bs = world.getBlock(coords)
    var depth: Int = bs.metadata & fluidLevelMask
    val blocks = BlockState.neighborOffsets.map(off => coords.offset(off._1, off._2, off._3))
    val bottomCoords = blocks.find(_.y == coords.y - 1).get
    val bottomBS = Some(world.getBlock(bottomCoords)).filter(_.blockType != Block.Air)//TODO: clean up
    if (!bottomBS.exists(_.blockType != Block.Air)) {
      world.setBlock(bottomCoords, new BlockState(this, depth.toByte))
      depth = fluidLevelMask
    } else if (bottomBS.exists(_.blockType == this) && bottomBS.get.metadata != 0) {
      val totalLevel = (fluidLevelMask - depth) + (fluidLevelMask - (bottomBS.get.metadata & fluidLevelMask))
      world.setBlock(bottomCoords, new BlockState(this, (fluidLevelMask - math.min(totalLevel, fluidLevelMask)).toByte))
      depth = fluidLevelMask - math.max(totalLevel - fluidLevelMask, 0)
    } else {
      blocks.filter(_.y == coords.y).map(c => (c, world.getBlock(c))).foreach { case (nCoords, ns) =>
        if (ns.blockType == Block.Air) {
          val belowNeighborBlock = world.getBlock(nCoords.offset(0, -1, 0))
          val belowNeighbor = belowNeighborBlock.blockType
          if (depth < 0x1e || (depth == 0x1e && (belowNeighbor == Block.Air || (belowNeighbor == this && belowNeighborBlock.metadata != 0)))) {
            world.setBlock(nCoords, new BlockState(this, 0x1e.toByte))
            depth += 1
          }
        } else if (ns.blockType == this) {
          val nsDepth: Int = ns.metadata & fluidLevelMask
          if (depth < fluidLevelMask) {
            if (nsDepth - 1 > depth) {
              world.setBlock(nCoords, new BlockState(this, (nsDepth - 1).toByte))
              depth += 1
            }
          }
        }
      }
    }

    if (depth >= fluidLevelMask)
      world.removeBlock(coords)
    else if (depth != (bs.metadata & fluidLevelMask))
      world.setBlock(coords, new BlockState(this, depth.toByte))
  }

  override def isTransparent(blockState: BlockState, side: Int): Boolean = blockState.metadata != 0

  override def blockHeight(blockState: BlockState): Float = 1f - (blockState.metadata & fluidLevelMask) / fluidLevelMask.toFloat
}