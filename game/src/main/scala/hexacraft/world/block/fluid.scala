package hexacraft.world.block

import hexacraft.physics.Viscosity
import hexacraft.world.CylinderSize
import hexacraft.world.coord.{BlockRelWorld, NeighborOffsets}

class BlockFluid(_id: Byte, _name: String, _displayName: String) extends Block(_id, _name, _displayName) {
  override val behaviour: Option[BlockBehaviour] = Some(new BlockBehaviourFluid)

  override def isCovering(metadata: Byte, side: Int): Boolean = false

  override def isTransmissive: Boolean = true

  override def isSolid: Boolean = false

  override def viscosity: Viscosity = Viscosity.water

  override def blockHeight(metadata: Byte): Float = {
    1f - (metadata & BlockBehaviourFluid.fluidLevelMask) / (BlockBehaviourFluid.fluidLevelMask + 1).toFloat
  }
}

object BlockBehaviourFluid {
  val fluidLevelMask = 0x1f
}

class BlockBehaviourFluid extends BlockBehaviour {
  private val fluidLevelMask = BlockBehaviourFluid.fluidLevelMask

  override def onUpdated(coords: BlockRelWorld, block: Block, world: BlockRepository)(using
      cylSize: CylinderSize
  ): Unit = {
    val bs = world.getBlock(coords)
    var depth: Int = bs.metadata & fluidLevelMask
    val bottomCoords = coords.offset(0, -1, 0)
    val bottomBS = world.getBlock(bottomCoords)
    if bottomBS.blockType == Block.Air then {
      world.setBlock(bottomCoords, new BlockState(block, depth.toByte))
      depth = fluidLevelMask + 1
    } else if bottomBS.blockType == block && bottomBS.metadata != 0 then {
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
      for off <- NeighborOffsets.all if off.dy == 0 do {
        val nCoords = coords.offset(off)
        val ns = world.getBlock(nCoords)

        if ns.blockType == Block.Air then {
          val belowNeighborBlock = world.getBlock(nCoords.offset(0, -1, 0))
          val belowNeighbor = belowNeighborBlock.blockType
          if depth < 0x1f || (depth == 0x1f && (belowNeighbor == Block.Air || (belowNeighbor == block && belowNeighborBlock.metadata != 0)))
          then {
            world.setBlock(nCoords, new BlockState(block, 0x1f.toByte))
            depth += 1
          }
        } else if ns.blockType == block then {
          val nsDepth: Int = ns.metadata & fluidLevelMask
          if depth < fluidLevelMask then {
            if nsDepth - 1 > depth then {
              world.setBlock(nCoords, new BlockState(block, (nsDepth - 1).toByte))
              depth += 1
            }
          }
        }
      }
    }

    if depth > fluidLevelMask then {
      world.removeBlock(coords)
    } else if depth != (bs.metadata & fluidLevelMask) then {
      world.setBlock(coords, new BlockState(block, depth.toByte))
    }
  }
}
