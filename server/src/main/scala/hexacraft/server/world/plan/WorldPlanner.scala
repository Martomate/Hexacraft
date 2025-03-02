package hexacraft.server.world.plan

import hexacraft.util.InlinedIterable
import hexacraft.world.{BlocksInWorldExtended, CylinderSize}
import hexacraft.world.chunk.Chunk
import hexacraft.world.coord.ChunkRelWorld
import hexacraft.world.entity.{Entity, EntityFactory}

class WorldPlanner(world: BlocksInWorldExtended, mainSeed: Long)(using CylinderSize) {
  private val planners: Seq[WorldFeaturePlanner] = Seq(
    new TreePlanner(world, mainSeed),
    new EntityGroupPlanner(world, pos => EntityFactory.atStartPos(Entity.getNextId, pos, "sheep").unwrap(), mainSeed)
  )

  def decorate(chunkCoords: ChunkRelWorld, chunk: Chunk): Unit = {
    if !chunk.isDecorated then {
      for p <- InlinedIterable(planners) do {
        p.decorate(chunkCoords, chunk)
      }
      chunk.setDecorated()
    }
  }

  def prepare(coords: ChunkRelWorld): Unit = {
    for ch <- coords.extendedNeighbors(4) do {
      for p <- InlinedIterable(planners) do {
        p.plan(ch)
      }
    }
  }
}
