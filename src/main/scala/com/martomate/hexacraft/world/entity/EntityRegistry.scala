package com.martomate.hexacraft.world.entity

import com.martomate.hexacraft.world.entity.EntityFactory

trait EntityRegistry {
  def get(name: String): Option[EntityFactory]
}

object EntityRegistry {
  def empty: EntityRegistry = _ => None

  def from(mappings: Map[String, EntityFactory]): EntityRegistry = name => mappings.get(name)
}
