package com.martomate.hexacraft.world.gen

import com.flowpowered.nbt.CompoundTag
import com.martomate.hexacraft.world.{BlocksInWorld, CylinderSize, World}
import com.martomate.hexacraft.world.block.Blocks
import com.martomate.hexacraft.world.chunk.Chunk
import com.martomate.hexacraft.world.entity.EntityRegistry
import com.martomate.hexacraft.world.gen.planner.{EntityGroupPlanner, TreePlanner, WorldFeaturePlanner}

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
