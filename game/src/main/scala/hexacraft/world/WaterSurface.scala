package hexacraft.world

import hexacraft.world.block.Block
import hexacraft.world.coord.{BlockRelWorld, CoordUtils, CylCoords, NeighborOffsets}

import scala.collection.mutable

object WaterSurface {

  /** How many water blocks to look through when searching for the surface */
  private val maxSearchedBlocks = 1024

  /** How far above the water surface (in CylCoords) a position may be for the surface to still be found.
    *
    * It has to be larger than the near plane of the camera, since the closest part of the water surface is not drawn
    * when the eye is closer to it than that. It should still be small, since only the water right below is known.
    */
  val maxHeightAbove: Double = 0.1

  /** Finds the height (in CylCoords) of the surface of the water at the given position.
    *
    * The surface is only found if the position is under water, or at most `maxHeightAbove` above the surface.
    */
  def heightNear(position: CylCoords, world: BlocksInWorld)(using CylinderSize): Option[Double] = {
    val (coords, _) = CoordUtils.getEnclosingBlock(position.toBlockCoords)

    val waterBlock =
      if isWater(world, coords) then Some(coords)
      else Some(coords.offset(0, -1, 0)).filter(c => isWater(world, c))

    waterBlock.map(c => surfaceOfWaterAt(c, world)).filter(h => position.y - h <= maxHeightAbove)
  }

  /** Returns the height of the surface of the body of water that the given (water) block is part of.
    *
    * The search goes through connected water blocks, highest first, until it finds water with air above it. This way
    * the surface is found even if there are blocks right above (e.g. when swimming under a ledge). If there is no such
    * water nearby (e.g. in a flooded cave) the top of the highest water block that was found is used instead.
    */
  private def surfaceOfWaterAt(start: BlockRelWorld, world: BlocksInWorld)(using CylinderSize): Double = {
    val visited = mutable.HashSet(start.value)
    val queue = mutable.PriorityQueue(start)(using Ordering.by[BlockRelWorld, Int](_.y))
    var highestTop = Double.NegativeInfinity

    while queue.nonEmpty do {
      val coords = queue.dequeue()
      val top = topOfWater(coords, world)
      highestTop = math.max(highestTop, top)

      if world.getBlock(coords.offset(0, 1, 0)).blockType == Block.Air then {
        return top
      }

      if visited.size < maxSearchedBlocks then {
        for off <- NeighborOffsets.all do {
          val neighbor = coords.offset(off)
          if !visited.contains(neighbor.value) && isWater(world, neighbor) then {
            visited += neighbor.value
            queue.enqueue(neighbor)
          }
        }
      }
    }

    highestTop
  }

  private def topOfWater(coords: BlockRelWorld, world: BlocksInWorld)(using CylinderSize): Double = {
    val block = world.getBlock(coords)
    (coords.y + block.blockType.blockHeight(block.metadata)) * 0.5
  }

  private def isWater(world: BlocksInWorld, coords: BlockRelWorld): Boolean = {
    world.getBlock(coords).blockType == Block.Water
  }
}
