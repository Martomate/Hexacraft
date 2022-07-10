package com.martomate.hexacraft.world.entity

import com.martomate.hexacraft.world.BlocksInWorld

trait EntityFactory {
  def createEntity(world: BlocksInWorld): Entity
}
