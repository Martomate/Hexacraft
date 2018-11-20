package com.martomate.hexacraft.world.entity

import scala.collection.mutable

object EntityLoader {
  private val registry: mutable.Map[String, EntityFactory] = mutable.Map.empty

  def register(name: String, maker: EntityFactory): Unit = {
    registry(name) = maker
  }

  def load(name: String): Option[Entity] = {
    registry.get(name).map(_.makeEntity())
  }
}
