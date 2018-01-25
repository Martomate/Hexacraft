package hexagon.block

import hexagon.HexBox
import hexagon.world.coord.BlockRelWorld

object Block {
  private val maxBlocks = 256
  
  private val blocks = new Array[Block](maxBlocks)
  def byId(id: Byte): Block = blocks(id)
  
  val Air       = BlockAir
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
  
  protected val texture = new BlockTexture(name)
  def bounds(blockState: BlockState) = new HexBox(0.5f, 0, 0.5f * blockHeight(blockState))
  
  def blockTex(side: Int): Int = texture.indices(side)
  def canBeRendered: Boolean = true

  def isTransparent: Boolean = false

  def blockHeight(blockState: BlockState): Float = 1.0f

  final def doUpdate(coords: BlockRelWorld): Unit = onUpdated(coords)
  protected def onUpdated(coords: BlockRelWorld): Unit = ()
}

object BlockAir extends Block(0, "air", "Air") {
  override def canBeRendered: Boolean = false
  override def isTransparent: Boolean = true
}

class BlockFluid(_id: Byte, _name: String, _displayName: String) extends Block(_id, _name, _displayName) {
  override def onUpdated(coords: BlockRelWorld): Unit = {
    val world = coords.world
    val blocks = BlockState.neighborOffsets.map(off => BlockRelWorld(coords.x + off._1, coords.y + off._2, coords.z + off._3, world)).filter(_.y <= coords.y).sortBy(_.y)
    blocks.find(c => world.getBlock(c).forall(_.blockType == Block.Air)).foreach(c => {
      //world.setBlock(new BlockState(c, this))
      //world.removeBlock(coords)
    })
  }

  override def isTransparent: Boolean = true

  override def blockHeight(blockState: BlockState): Float = 0.75f

  override def canBeRendered: Boolean = false
}