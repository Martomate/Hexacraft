package hexacraft.world

import hexacraft.world.block.Block
import hexacraft.world.coord.{BlockRelWorld, CoordUtils, CylCoords, NeighborOffsets}

import scala.collection.mutable

object WaterSurface {

  /** How many water blocks to look through when searching for the surface */
  private val maxSearchedBlocks = 1024

  /** Finds the height (in CylCoords) of the surface of the water at the given position, if it is in a water block.
    *
    * Note: the position can be above the surface if the water block is not full.
    */
  def heightAt(position: CylCoords, world: BlocksInWorld)(using CylinderSize): Option[Double] = {
    val (coords, _) = CoordUtils.getEnclosingBlock(position.toBlockCoords)

    if isWater(world, coords) then Some(surfaceOfWaterAt(coords, world)) else None
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
