package com.martomate.hexacraft.block

trait EmittingLight extends Block {
  override def lightEmitted: Byte = 14
}
