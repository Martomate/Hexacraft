package com.martomate.hexacraft.world.block

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.behaviour.{BlockBehaviour, BlockBehaviourNothing}
import com.martomate.hexacraft.world.block.setget.BlockSetAndGet
import com.martomate.hexacraft.world.coord.integer.BlockRelWorld

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
  final def doUpdate(coords: BlockRelWorld, world: BlockSetAndGet)(implicit cylSize: CylinderSize): Unit = behaviour.onUpdated(coords, world)

  override def equals(o: Any): Boolean = o match {
    case other : Block =>
      id == other.id
    case _ => false
  }
}
