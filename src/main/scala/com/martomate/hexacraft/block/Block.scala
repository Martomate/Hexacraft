package com.martomate.hexacraft.block

import com.martomate.hexacraft.HexBox
import com.martomate.hexacraft.world.coord.BlockRelWorld
import com.martomate.hexacraft.world.storage.BlockSetAndGet

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
