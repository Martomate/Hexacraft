package com.martomate.hexacraft.world.block.fluid

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.behaviour.BlockBehaviour
import com.martomate.hexacraft.world.block.{Block, BlockSetAndGet, BlockState, Blocks}
import com.martomate.hexacraft.world.coord.integer.{BlockRelWorld, NeighborOffsets}

class BlockBehaviourFluid(block: Block) extends BlockBehaviour {
  private val fluidLevelMask = BlockBehaviourFluid.fluidLevelMask

  override def onUpdated(coords: BlockRelWorld, world: BlockSetAndGet)(implicit
      cylSize: CylinderSize
  ): Unit = {
    val bs = world.getBlock(coords)
    var depth: Int = bs.metadata & fluidLevelMask
    val bottomCoords = coords.offset(0, -1, 0)
    val bottomBS = world.getBlock(bottomCoords)
    if (bottomBS.blockType == Blocks.Air) {
      world.setBlock(bottomCoords, new BlockState(block, depth.toByte))
      depth = fluidLevelMask + 1
    } else if (bottomBS.blockType == block && bottomBS.metadata != 0) {
      val fluidHere = fluidLevelMask + 1 - depth
      val fluidBelow = fluidLevelMask + 1 - (bottomBS.metadata & fluidLevelMask)

      val totalFluid = fluidHere + fluidBelow
      val fluidBelowAfter = math.min(totalFluid, fluidLevelMask + 1)
      val fluidHereAfter = totalFluid - fluidBelowAfter

      world.setBlock(
        bottomCoords,
        new BlockState(block, (fluidLevelMask + 1 - fluidBelowAfter).toByte)
      )
      depth = fluidLevelMask + 1 - fluidHereAfter
    } else {
      for (off <- NeighborOffsets.all if off.dy == 0) {
        val nCoords = coords.offset(off)
        val ns = world.getBlock(nCoords)

        if (ns.blockType == Blocks.Air) {
          val belowNeighborBlock = world.getBlock(nCoords.offset(0, -1, 0))
          val belowNeighbor = belowNeighborBlock.blockType
          if (
            depth < 0x1f || (depth == 0x1f && (belowNeighbor == Blocks.Air || (belowNeighbor == block && belowNeighborBlock.metadata != 0)))
          ) {
            world.setBlock(nCoords, new BlockState(block, 0x1f.toByte))
            depth += 1
          }
        } else if (ns.blockType == block) {
          val nsDepth: Int = ns.metadata & fluidLevelMask
          if (depth < fluidLevelMask) {
            if (nsDepth - 1 > depth) {
              world.setBlock(nCoords, new BlockState(block, (nsDepth - 1).toByte))
              depth += 1
            }
          }
        }
      }
    }

    if (depth > fluidLevelMask)
      world.removeBlock(coords)
    else if (depth != (bs.metadata & fluidLevelMask))
      world.setBlock(coords, new BlockState(block, depth.toByte))
  }
}

object BlockBehaviourFluid {
  val fluidLevelMask = 0x1f
}
