package com.martomate.hexacraft.world.entity.sheep.ai

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.Block
import com.martomate.hexacraft.world.coord.CoordUtils
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.coord.integer.BlockRelWorld
import com.martomate.hexacraft.world.entity.ai.EntityAIInput
import com.martomate.hexacraft.world.entity.sheep.SheepEntity
import com.martomate.hexacraft.world.worldlike.BlocksInWorld

class SimpleSheepAIInput(world: BlocksInWorld, sheep: SheepEntity)(implicit cylSize: CylinderSize) extends EntityAIInput {
  def blockInFront(dist: Double): Block = world.getBlock(coordsAtOffset(dist * math.cos(sheep.rotation.y), 0, dist * -math.sin(sheep.rotation.y))).blockType

  private def coordsAtOffset(dx: Double, dy: Double, dz: Double): BlockRelWorld =
    CoordUtils.getEnclosingBlock(CylCoords(sheep.position.x + dx, sheep.position.y + dy, sheep.position.z + dz).toBlockCoords)._1
}
