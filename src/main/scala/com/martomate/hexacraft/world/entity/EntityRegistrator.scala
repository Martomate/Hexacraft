package com.martomate.hexacraft.world.entity

import com.martomate.hexacraft.world.worldlike.IWorld

object EntityRegistrator {
  def load(world: IWorld)(implicit modelFactory: EntityModelLoader): Unit = {
    EntityLoader.register("player", new PlayerEntityFactory(world))
  }
}
