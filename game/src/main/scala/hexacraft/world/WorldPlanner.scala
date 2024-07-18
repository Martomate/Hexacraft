package hexacraft.world

import hexacraft.world.chunk.Chunk
import hexacraft.world.coord.ChunkRelWorld
import hexacraft.world.entity.{Entity, EntityFactory}
import hexacraft.world.gen.{EntityGroupPlanner, TreePlanner, WorldFeaturePlanner}

class WorldPlanner(world: BlocksInWorldExtended, mainSeed: Long)(using CylinderSize) {
  private val planners: Seq[WorldFeaturePlanner] = Seq(
    new TreePlanner(world, mainSeed),
    new EntityGroupPlanner(world, pos => EntityFactory.atStartPos(Entity.getNextId, pos, "sheep").unwrap(), mainSeed)
  )

  def decorate(chunkCoords: ChunkRelWorld, chunk: Chunk): Unit = {
    if !chunk.isDecorated then {
      val pIt = planners.iterator
      while pIt.hasNext do {
        val p = pIt.next
        p.decorate(chunkCoords, chunk)
      }
      chunk.setDecorated()
    }
  }

  def prepare(coords: ChunkRelWorld): Unit = {
    val neighbors = coords.extendedNeighbors(4)
    val nIt = neighbors.iterator
    while nIt.hasNext do {
      val ch = nIt.next
      val pIt = planners.iterator
      while pIt.hasNext do {
        pIt.next.plan(ch)
      }
    }
  }
}
