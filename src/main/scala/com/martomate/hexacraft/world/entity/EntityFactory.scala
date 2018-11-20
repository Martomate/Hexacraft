package com.martomate.hexacraft.world.entity

import com.martomate.hexacraft.world.entity.player.PlayerEntity
import com.martomate.hexacraft.world.worldlike.IWorld

trait EntityFactory {
  def makeEntity(): Entity
}

class PlayerEntityFactory(world: IWorld)(implicit modelFactory: EntityModelLoader) extends EntityFactory {
  override def makeEntity(): PlayerEntity = new PlayerEntity(modelFactory.load("player"), world)
}
