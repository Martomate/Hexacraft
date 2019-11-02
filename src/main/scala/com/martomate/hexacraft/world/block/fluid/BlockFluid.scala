package com.martomate.hexacraft.world.block.fluid

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.Block
import com.martomate.hexacraft.world.block.behaviour.BlockBehaviour

class BlockFluid(_id: Byte, _name: String, _displayName: String) extends Block(_id, _name, _displayName) {
  override protected val behaviour: BlockBehaviour = new BlockBehaviourFluid(this)

  override def isTransparent(metadata: Byte, side: Int): Boolean = metadata != 0

  override def blockHeight(metadata: Byte): Float =
    1f - (metadata & BlockBehaviourFluid.fluidLevelMask) / BlockBehaviourFluid.fluidLevelMask.toFloat
}
