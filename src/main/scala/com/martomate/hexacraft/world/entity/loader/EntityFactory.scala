package com.martomate.hexacraft.world.entity.loader

import com.martomate.hexacraft.world.entity.player.PlayerEntity
import com.martomate.hexacraft.world.entity.player.ai.PlayerAIFactory
import com.martomate.hexacraft.world.entity.Entity
import com.martomate.hexacraft.world.entity.sheep.SheepEntity
import com.martomate.hexacraft.world.entity.sheep.ai.SheepAIFactory
import com.martomate.hexacraft.world.worldlike.IWorld

trait EntityFactory {
  def makeEntity(world: IWorld): Entity
}

class PlayerEntityFactory(implicit modelFactory: EntityModelLoader) extends EntityFactory {
  override def makeEntity(world: IWorld): PlayerEntity = new PlayerEntity(modelFactory.load("player"), world, PlayerAIFactory)
}

class SheepEntityFactory(implicit modelFactory: EntityModelLoader) extends EntityFactory {
  override def makeEntity(world: IWorld): SheepEntity = new SheepEntity(modelFactory.load("sheep"), world, SheepAIFactory)
}
