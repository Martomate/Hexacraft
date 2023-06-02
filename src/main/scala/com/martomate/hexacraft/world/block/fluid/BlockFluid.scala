package com.martomate.hexacraft.world.block.fluid

import com.martomate.hexacraft.world.block.{Block, BlockBehaviour, BlockLoader, Blocks}

class BlockFluid(_id: Byte, _name: String, _displayName: String) extends Block(_id, _name, _displayName) {
  override val behaviour: Option[BlockBehaviour] = Some(new BlockBehaviourFluid)

  override def isTransparent(metadata: Byte, side: Int): Boolean = metadata != 0

  override def blockHeight(metadata: Byte): Float =
    1f - (metadata & BlockBehaviourFluid.fluidLevelMask) / (BlockBehaviourFluid.fluidLevelMask + 1).toFloat
}
