package com.martomate.hexacraft.world.entity.loader

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.BlocksInWorld
import com.martomate.hexacraft.world.entity.Entity
import com.martomate.hexacraft.world.entity.player.{PlayerAIFactory, PlayerEntity}
import com.martomate.hexacraft.world.entity.sheep.{SheepAIFactory, SheepEntity}

trait EntityFactory {
  def makeEntity(world: BlocksInWorld): Entity
}

class PlayerEntityFactory(implicit modelFactory: EntityModelLoader, cylSizeImpl: CylinderSize) extends EntityFactory {
  override def makeEntity(world: BlocksInWorld): PlayerEntity = new PlayerEntity(modelFactory.load("player"), world, PlayerAIFactory)
}

class SheepEntityFactory(implicit modelFactory: EntityModelLoader, cylSizeImpl: CylinderSize) extends EntityFactory {
  override def makeEntity(world: BlocksInWorld): SheepEntity = new SheepEntity(modelFactory.load("sheep"), world, SheepAIFactory)
}
