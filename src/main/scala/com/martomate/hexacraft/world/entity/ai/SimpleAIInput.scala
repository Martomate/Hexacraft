package com.martomate.hexacraft.world.entity.ai

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.BlocksInWorld
import com.martomate.hexacraft.world.block.Block
import com.martomate.hexacraft.world.coord.CoordUtils
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.coord.integer.BlockRelWorld
import com.martomate.hexacraft.world.entity.Entity

class SimpleAIInput[E <: Entity](world: BlocksInWorld, entity: Entity)(implicit cylSize: CylinderSize) extends EntityAIInput {
  def blockInFront(dist: Double): Block = world.getBlock(coordsAtOffset(dist * math.cos(entity.rotation.y), 0, dist * -math.sin(entity.rotation.y))).blockType

  private def coordsAtOffset(dx: Double, dy: Double, dz: Double): BlockRelWorld =
    CoordUtils.getEnclosingBlock(CylCoords(entity.position.x + dx, entity.position.y + dy, entity.position.z + dz).toBlockCoords)._1
}
