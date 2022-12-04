package com.martomate.hexacraft.world.gen

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.BlocksInWorld
import com.martomate.hexacraft.world.block.Blocks
import com.martomate.hexacraft.world.chunk.{Chunk, ChunkAddedOrRemovedListener}
import com.martomate.hexacraft.world.entity.EntityRegistry
import com.martomate.hexacraft.world.gen.planner.{EntityGroupPlanner, TreePlanner, WorldFeaturePlanner}

import com.flowpowered.nbt.CompoundTag

class WorldPlanner(world: BlocksInWorld, registry: EntityRegistry, mainSeed: Long)(using
    CylinderSize,
    Blocks
) extends ChunkAddedOrRemovedListener {
  private val planners: Seq[WorldFeaturePlanner] = Seq(
    new TreePlanner(world, mainSeed),
    new EntityGroupPlanner(world, registry.get("sheep").get, mainSeed)
  )

  def decorate(chunk: Chunk): Unit = {
    if (!chunk.isDecorated) {
      planners.foreach(_.decorate(chunk))
      chunk.setDecorated()
    }
  }

  override def onChunkAdded(chunk: Chunk): Unit = {
    for (ch <- chunk.coords.extendedNeighbors(1))
      for (p <- planners)
        p.plan(ch)
  }

  override def onChunkRemoved(chunk: Chunk): Unit = ()
}

object WorldPlanner {
  def apply(world: BlocksInWorld, registry: EntityRegistry, mainSeed: Long)(using
      CylinderSize,
      Blocks
  ): WorldPlanner = new WorldPlanner(world, registry, mainSeed)
}
