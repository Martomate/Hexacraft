package com.martomate.hexacraft.world.entity.registry

import com.martomate.hexacraft.world.entity.loader.{EntityModelLoader, PlayerEntityFactory, SheepEntityFactory}

object EntityRegistrator {
  def load()(implicit modelFactory: EntityModelLoader): Unit = {
    EntityRegistry.register("player", new PlayerEntityFactory())
    EntityRegistry.register("sheep", new SheepEntityFactory())
  }
}
