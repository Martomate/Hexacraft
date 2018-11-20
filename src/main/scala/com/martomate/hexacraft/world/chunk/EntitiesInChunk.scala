package com.martomate.hexacraft.world.chunk

import com.flowpowered.nbt.{CompoundTag, Tag}
import com.martomate.hexacraft.world.entity.Entity

trait EntitiesInChunk {
  def fromNBT(nbt: CompoundTag): Unit
  def toNBT: Seq[Tag[_]]

  def +=(entity: Entity): Unit
  def -=(entity: Entity): Unit

  def count: Int
  def allEntities: Iterable[Entity]

  def needsToSave: Boolean
}
