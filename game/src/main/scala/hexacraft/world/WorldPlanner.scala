package hexacraft.world

import hexacraft.world.chunk.Chunk
import hexacraft.world.coord.ChunkRelWorld
import hexacraft.world.entity.EntityFactory
import hexacraft.world.gen.{EntityGroupPlanner, TreePlanner, WorldFeaturePlanner}

class WorldPlanner(world: BlocksInWorldExtended, mainSeed: Long)(using CylinderSize) {
  private val planners: Seq[WorldFeaturePlanner] = Seq(
    new TreePlanner(world, mainSeed),
    new EntityGroupPlanner(world, pos => EntityFactory.atStartPos(pos, "sheep").unwrap(), mainSeed)
  )

  def decorate(chunkCoords: ChunkRelWorld, chunk: Chunk): Unit = {
    if !chunk.isDecorated then {
      for p <- planners do {
        p.decorate(chunkCoords, chunk)
      }
      chunk.setDecorated()
    }
  }

  def prepare(coords: ChunkRelWorld): Unit = {
    for {
      ch <- coords.extendedNeighbors(4)
      p <- planners
    } do {
      p.plan(ch)
    }
  }
}
