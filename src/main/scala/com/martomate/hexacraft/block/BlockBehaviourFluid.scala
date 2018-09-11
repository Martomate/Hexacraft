package com.martomate.hexacraft.block

import com.martomate.hexacraft.world.coord.integer.BlockRelWorld
import com.martomate.hexacraft.world.storage.BlockSetAndGet

class BlockBehaviourFluid(block: Block) extends BlockBehaviour {
  private val fluidLevelMask = BlockBehaviourFluid.fluidLevelMask

  override def onUpdated(coords: BlockRelWorld, world: BlockSetAndGet): Unit = {
    val bs = world.getBlock(coords)
    var depth: Int = bs.metadata & fluidLevelMask
    val blocks = BlockState.neighborOffsets.map(off => coords.offset(off._1, off._2, off._3))
    val bottomCoords = blocks.find(_.y == coords.y - 1).get
    val bottomBS = Some(world.getBlock(bottomCoords)).filter(_.blockType != Blocks.Air)//TODO: clean up
    if (!bottomBS.exists(_.blockType != Blocks.Air)) {
      world.setBlock(bottomCoords, new BlockState(block, depth.toByte))
      depth = fluidLevelMask
    } else if (bottomBS.exists(_.blockType == block) && bottomBS.get.metadata != 0) {
      val totalLevel = (fluidLevelMask - depth) + (fluidLevelMask - (bottomBS.get.metadata & fluidLevelMask))
      world.setBlock(bottomCoords, new BlockState(block, (fluidLevelMask - math.min(totalLevel, fluidLevelMask)).toByte))
      depth = fluidLevelMask - math.max(totalLevel - fluidLevelMask, 0)
    } else {
      blocks.filter(_.y == coords.y).map(c => (c, world.getBlock(c))).foreach { case (nCoords, ns) =>
        if (ns.blockType == Blocks.Air) {
          val belowNeighborBlock = world.getBlock(nCoords.offset(0, -1, 0))
          val belowNeighbor = belowNeighborBlock.blockType
          if (depth < 0x1e || (depth == 0x1e && (belowNeighbor == Blocks.Air || (belowNeighbor == block && belowNeighborBlock.metadata != 0)))) {
            world.setBlock(nCoords, new BlockState(block, 0x1e.toByte))
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

    if (depth >= fluidLevelMask)
      world.removeBlock(coords)
    else if (depth != (bs.metadata & fluidLevelMask))
      world.setBlock(coords, new BlockState(block, depth.toByte))
  }
}

object BlockBehaviourFluid {
  val fluidLevelMask = 0x1f
}