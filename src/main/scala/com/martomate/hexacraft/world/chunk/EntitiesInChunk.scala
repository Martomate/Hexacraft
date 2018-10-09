package com.martomate.hexacraft.world.chunk

import com.martomate.hexacraft.world.entity.Entity

trait EntitiesInChunk {
  def +=(entity: Entity): Unit
  def -=(entity: Entity): Unit

  def count: Int
  def allEntities: Iterable[Entity]
}
