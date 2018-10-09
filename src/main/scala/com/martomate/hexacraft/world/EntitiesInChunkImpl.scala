package com.martomate.hexacraft.world

import com.martomate.hexacraft.world.chunk.EntitiesInChunk
import com.martomate.hexacraft.world.entity.Entity

import scala.collection.mutable

class EntitiesInChunkImpl extends EntitiesInChunk {
  private val entities: mutable.Set[Entity] = mutable.Set.empty

  override def +=(entity: Entity): Unit = entities += entity

  override def -=(entity: Entity): Unit = entities -= entity

  override def count: Int = entities.size

  override def allEntities: Iterable[Entity] = entities
}
