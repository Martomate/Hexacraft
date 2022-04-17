package com.martomate.hexacraft.world.entity.ai

import com.martomate.hexacraft.world.block.Block
import com.martomate.hexacraft.world.coord.fp.CylCoords
import org.joml.Vector3d

trait EntityAIInput {
  def blockInFront(position: CylCoords, rotation: Vector3d, dist: Double): Block
}
