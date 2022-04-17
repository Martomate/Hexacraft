package com.martomate.hexacraft.world.entity.registry

import com.martomate.hexacraft.world.BlocksInWorld
import com.martomate.hexacraft.world.entity.Entity

import scala.collection.mutable

object EntityRegistry {
  type EntityFactory = BlocksInWorld => Entity

  private val registry: mutable.Map[String, EntityFactory] = mutable.Map.empty

  def register(name: String, maker: EntityFactory): Unit = {
    registry(name) = maker
  }

  def load(name: String, world: BlocksInWorld): Option[Entity] = {
    registry.get(name).map(_.apply(world))
  }
}
