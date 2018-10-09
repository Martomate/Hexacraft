package com.martomate.hexacraft.world.entity

import com.martomate.hexacraft.world.coord.fp.CylCoords

class TempEntity(initPos: CylCoords, override val model: EntityModel) extends Entity(initPos) {
  def tempTick(): Unit = {
    rotation.y += 0.01f
    rotation.z += 0.01f
    rotation.x += 0.01f
  }
}
