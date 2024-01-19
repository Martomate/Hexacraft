package hexacraft.world

import hexacraft.world.chunk.Chunk
import hexacraft.world.entity.EntityFactory
import hexacraft.world.gen.{EntityGroupPlanner, TreePlanner, WorldFeaturePlanner}

class WorldPlanner(world: BlocksInWorld, mainSeed: Long)(using CylinderSize) {
  private val planners: Seq[WorldFeaturePlanner] = Seq(
    new TreePlanner(world, mainSeed),
    new EntityGroupPlanner(world, pos => EntityFactory.atStartPos(pos, "sheep").unwrap(), mainSeed)
  )

  def decorate(chunk: Chunk): Unit = {
    if !chunk.isDecorated then {
      for p <- planners do {
        p.decorate(chunk)
      }
      chunk.setDecorated()
    }
  }

  def onWorldEvent(event: World.Event): Unit = {
    event match {
      case World.Event.ChunkAdded(coords) =>
        for {
          ch <- coords.extendedNeighbors(1)
          p <- planners
        } do {
          p.plan(ch)
        }
      case World.Event.ChunkRemoved(_) =>
      case _                           =>
    }
  }
}
