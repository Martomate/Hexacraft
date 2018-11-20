package com.martomate.hexacraft.util

import com.flowpowered.nbt.{CompoundTag, Tag}

trait NBTSavable {
  def fromNBT(tag: CompoundTag): Unit
  def toNBT: Seq[Tag[_]]
}
