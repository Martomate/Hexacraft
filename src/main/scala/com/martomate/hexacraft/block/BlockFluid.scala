package com.martomate.hexacraft.block

import com.martomate.hexacraft.world.coord.integer.BlockRelWorld
import com.martomate.hexacraft.world.storage.BlockSetAndGet

class BlockFluid(_id: Byte, _name: String, _displayName: String) extends Block(_id, _name, _displayName) {
  private val fluidLevelMask = 0x1f

  override def onUpdated(coords: BlockRelWorld, world: BlockSetAndGet): Unit = {
    val bs = world.getBlock(coords)
    var depth: Int = bs.metadata & fluidLevelMask
    val blocks = BlockState.neighborOffsets.map(off => coords.offset(off._1, off._2, off._3))
    val bottomCoords = blocks.find(_.y == coords.y - 1).get
    val bottomBS = Some(world.getBlock(bottomCoords)).filter(_.blockType != Blocks.Air)//TODO: clean up
    if (!bottomBS.exists(_.blockType != Blocks.Air)) {
      world.setBlock(bottomCoords, new BlockState(this, depth.toByte))
      depth = fluidLevelMask
    } else if (bottomBS.exists(_.blockType == this) && bottomBS.get.metadata != 0) {
      val totalLevel = (fluidLevelMask - depth) + (fluidLevelMask - (bottomBS.get.metadata & fluidLevelMask))
      world.setBlock(bottomCoords, new BlockState(this, (fluidLevelMask - math.min(totalLevel, fluidLevelMask)).toByte))
      depth = fluidLevelMask - math.max(totalLevel - fluidLevelMask, 0)
    } else {
      blocks.filter(_.y == coords.y).map(c => (c, world.getBlock(c))).foreach { case (nCoords, ns) =>
        if (ns.blockType == Blocks.Air) {
          val belowNeighborBlock = world.getBlock(nCoords.offset(0, -1, 0))
          val belowNeighbor = belowNeighborBlock.blockType
          if (depth < 0x1e || (depth == 0x1e && (belowNeighbor == Blocks.Air || (belowNeighbor == this && belowNeighborBlock.metadata != 0)))) {
            world.setBlock(nCoords, new BlockState(this, 0x1e.toByte))
            depth += 1
          }
        } else if (ns.blockType == this) {
          val nsDepth: Int = ns.metadata & fluidLevelMask
          if (depth < fluidLevelMask) {
            if (nsDepth - 1 > depth) {
              world.setBlock(nCoords, new BlockState(this, (nsDepth - 1).toByte))
              depth += 1
            }
          }
        }
      }
    }

    if (depth >= fluidLevelMask)
      world.removeBlock(coords)
    else if (depth != (bs.metadata & fluidLevelMask))
      world.setBlock(coords, new BlockState(this, depth.toByte))
  }

  override def isTransparent(blockState: BlockState, side: Int): Boolean = blockState.metadata != 0

  override def blockHeight(blockState: BlockState): Float = 1f - (blockState.metadata & fluidLevelMask) / fluidLevelMask.toFloat
}
