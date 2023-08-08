package hexacraft.world.gen

import hexacraft.world.{BlocksInWorld, CylinderSize, World}
import hexacraft.world.block.Blocks
import hexacraft.world.chunk.Chunk
import hexacraft.world.entity.EntityRegistry
import hexacraft.world.gen.planner.{EntityGroupPlanner, TreePlanner, WorldFeaturePlanner}

class WorldPlanner(world: BlocksInWorld, registry: EntityRegistry, mainSeed: Long)(using CylinderSize, Blocks):
  private val planners: Seq[WorldFeaturePlanner] = Seq(
    new TreePlanner(world, mainSeed),
    new EntityGroupPlanner(world, registry.get("sheep").get, mainSeed)
  )

  def decorate(chunk: Chunk): Unit =
    if !chunk.isDecorated then
      for p <- planners do p.decorate(chunk)
      chunk.setDecorated()

  def onWorldEvent(event: World.Event): Unit =
    event match
      case World.Event.ChunkAdded(chunk) =>
        for
          ch <- chunk.coords.extendedNeighbors(1)
          p <- planners
        do p.plan(ch)
      case World.Event.ChunkRemoved(_) =>

object WorldPlanner:
  def apply(world: BlocksInWorld, registry: EntityRegistry, mainSeed: Long)(using CylinderSize, Blocks): WorldPlanner =
    new WorldPlanner(world, registry, mainSeed)
