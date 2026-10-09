package hexacraft.world

import hexacraft.world.block.Block
import hexacraft.world.coord.{BlockRelWorld, CoordUtils, CylCoords}

object WaterSurface {

  /** How far above the water surface (in CylCoords) a position may be for the surface to still be found */
  val maxHeightAbove: Double = 1.0

  /** How many water blocks to look through when searching upwards for the surface */
  private val maxWaterDepth = 256

  /** Finds the height (in CylCoords) of the water surface in the column of the given position.
    *
    * The surface is only found if the position is in water, or at most `maxHeightAbove` above the surface.
    */
  def heightNear(position: CylCoords, world: BlocksInWorld)(using CylinderSize): Option[Double] = {
    val (coords, _) = CoordUtils.getEnclosingBlock(position.toBlockCoords)

    val surface =
      if isWater(world, coords) then {
        Some(surfaceOfWaterAt(coords, world))
      } else {
        findWaterBelow(coords, world).map(c => surfaceOfWaterAt(c, world))
      }

    surface.filter(h => position.y - h <= maxHeightAbove)
  }

  /** Returns the height of the top of the body of water that the given (water) block is part of */
  private def surfaceOfWaterAt(coords: BlockRelWorld, world: BlocksInWorld)(using CylinderSize): Double = {
    var top = coords
    var steps = 0
    while steps < maxWaterDepth && isWater(world, top.offset(0, 1, 0)) do {
      top = top.offset(0, 1, 0)
      steps += 1
    }

    val block = world.getBlock(top)
    (top.y + block.blockType.blockHeight(block.metadata)) * 0.5
  }

  /** Returns the first block below `coords` if it is water and there is only air in between */
  private def findWaterBelow(coords: BlockRelWorld, world: BlocksInWorld)(using CylinderSize): Option[BlockRelWorld] = {
    val maxSteps = math.ceil(maxHeightAbove * 2).toInt + 1

    var c = coords
    var steps = 0
    while steps < maxSteps do {
      c = c.offset(0, -1, 0)
      steps += 1

      val block = world.getBlock(c)
      if block.blockType == Block.Water then {
        return Some(c)
      } else if block.blockType != Block.Air then {
        return None
      }
    }
    None
  }

  private def isWater(world: BlocksInWorld, coords: BlockRelWorld): Boolean = {
    world.getBlock(coords).blockType == Block.Water
  }
}
