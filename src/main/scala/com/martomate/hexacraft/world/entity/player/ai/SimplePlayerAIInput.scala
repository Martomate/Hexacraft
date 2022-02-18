package com.martomate.hexacraft.world.entity.player.ai

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.BlocksInWorld
import com.martomate.hexacraft.world.block.Block
import com.martomate.hexacraft.world.coord.CoordUtils
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.coord.integer.BlockRelWorld
import com.martomate.hexacraft.world.entity.ai.EntityAIInput
import com.martomate.hexacraft.world.entity.player.PlayerEntity

class SimplePlayerAIInput(world: BlocksInWorld, player: PlayerEntity)(implicit cylSize: CylinderSize) extends EntityAIInput {
  def blockInFront(dist: Double): Block = world.getBlock(coordsAtOffset(dist * math.cos(player.rotation.y), 0, dist * -math.sin(player.rotation.y))).blockType

  private def coordsAtOffset(dx: Double, dy: Double, dz: Double): BlockRelWorld =
    CoordUtils.getEnclosingBlock(CylCoords(player.position.x + dx, player.position.y + dy, player.position.z + dz).toBlockCoords)._1
}
