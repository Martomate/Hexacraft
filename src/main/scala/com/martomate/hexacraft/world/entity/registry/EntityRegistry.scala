package com.martomate.hexacraft.world.entity.registry

import com.martomate.hexacraft.world.entity.Entity
import com.martomate.hexacraft.world.entity.loader.EntityFactory
import com.martomate.hexacraft.world.worldlike.IWorld

import scala.collection.mutable

object EntityRegistry {
  private val registry: mutable.Map[String, EntityFactory] = mutable.Map.empty

  def register(name: String, maker: EntityFactory): Unit = {
    registry(name) = maker
  }

  def load(name: String, world: IWorld): Option[Entity] = {
    registry.get(name).map(_.makeEntity(world))
  }
}
