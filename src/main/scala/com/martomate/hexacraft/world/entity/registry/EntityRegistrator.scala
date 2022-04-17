package com.martomate.hexacraft.world.entity.registry

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.entity.loader.EntityModelLoader
import com.martomate.hexacraft.world.entity.player.{PlayerAIFactory, PlayerEntity}
import com.martomate.hexacraft.world.entity.sheep.{SheepAIFactory, SheepEntity}

object EntityRegistrator {
  def load()(implicit modelFactory: EntityModelLoader, cylSizeImpl: CylinderSize): Unit = {
    EntityRegistry.register("player", world => new PlayerEntity(modelFactory.load("player"), world, PlayerAIFactory))
    EntityRegistry.register("sheep", world => new SheepEntity(modelFactory.load("sheep"), world, SheepAIFactory))
  }
}
