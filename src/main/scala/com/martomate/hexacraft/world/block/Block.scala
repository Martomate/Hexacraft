package com.martomate.hexacraft.world.block

import com.martomate.hexacraft.world.collision.HexBox
import com.martomate.hexacraft.world.coord.integer.BlockRelWorld
import com.martomate.hexacraft.world.temp.BlockSetAndGet

object Block {
  private val maxBlocks = 256

  private val blocks = new Array[Block](maxBlocks)
  def byId(id: Byte): Block = blocks(id)
}

class Block(val id: Byte, val name: String, val displayName: String) {
  Block.blocks(id) = this
  
  protected lazy val texture = new BlockTexture(name, BlockLoader)
  def bounds(metadata: Byte) = new HexBox(0.5f, 0, 0.5f * blockHeight(metadata))
  
  def blockTex(side: Int): Int = texture.indices(side)
  def canBeRendered: Boolean = true

  def isTransparent(metadata: Byte, side: Int): Boolean = false

  def lightEmitted: Byte = 0

  def blockHeight(metadata: Byte): Float = 1.0f

  protected val behaviour: BlockBehaviour = new BlockBehaviourNothing
  final def doUpdate(coords: BlockRelWorld, world: BlockSetAndGet): Unit = behaviour.onUpdated(coords, world)
}
