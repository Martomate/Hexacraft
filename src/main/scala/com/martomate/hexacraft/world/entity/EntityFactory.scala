package com.martomate.hexacraft.world.entity

import com.martomate.hexacraft.world.entity.player.PlayerEntity
import com.martomate.hexacraft.world.worldlike.IWorld

trait EntityFactory {
  def makeEntity(world: IWorld): Entity
}

class PlayerEntityFactory(implicit modelFactory: EntityModelLoader) extends EntityFactory {
  override def makeEntity(world: IWorld): PlayerEntity = new PlayerEntity(modelFactory.load("player"), world)
}
