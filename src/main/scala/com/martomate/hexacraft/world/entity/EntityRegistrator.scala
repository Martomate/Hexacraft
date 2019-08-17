package com.martomate.hexacraft.world.entity

object EntityRegistrator {
  def load()(implicit modelFactory: EntityModelLoader): Unit = {
    EntityLoader.register("player", new PlayerEntityFactory())
  }
}
