package com.martomate.hexacraft.world.entity.ai

import com.martomate.hexacraft.world.BlocksInWorld
import com.martomate.hexacraft.world.block.Block
import com.martomate.hexacraft.world.coord.CoordUtils
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.coord.integer.BlockRelWorld
import org.joml.Vector3d

class SimpleAIInput(world: BlocksInWorld) extends EntityAIInput {
  def blockInFront(position: CylCoords, rotation: Vector3d, dist: Double): Block = {
    val blockInFrontCoords =
      position.offset(dist * math.cos(rotation.y), 0, dist * -math.sin(rotation.y))
    world.getBlock(cylToBlockCoords(blockInFrontCoords)).blockType
  }

  private def cylToBlockCoords(coords: CylCoords): BlockRelWorld =
    CoordUtils.getEnclosingBlock(coords.toBlockCoords)._1
}
