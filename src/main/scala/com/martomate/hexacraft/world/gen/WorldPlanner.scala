package com.martomate.hexacraft.world.gen

import com.flowpowered.nbt.CompoundTag
import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.BlocksInWorld
import com.martomate.hexacraft.world.chunk.{Chunk, ChunkAddedOrRemovedListener}
import com.martomate.hexacraft.world.entity.registry.EntityRegistry
import com.martomate.hexacraft.world.gen.planner.{SheepPlanner, TreePlanner, WorldFeaturePlanner}

class WorldPlanner(world: BlocksInWorld, registry: EntityRegistry, mainSeed: Long)(implicit
    cylSize: CylinderSize
) extends ChunkAddedOrRemovedListener {
  private val planners: Seq[WorldFeaturePlanner] = Seq(
    new TreePlanner(world, mainSeed),
    new SheepPlanner(world, registry.get("sheep").get, mainSeed)
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
  def apply(world: BlocksInWorld, registry: EntityRegistry, mainSeed: Long, nbt: CompoundTag)(
      implicit cylSize: CylinderSize
  ): WorldPlanner = {
    val wp = new WorldPlanner(world, registry, mainSeed)

    wp
  }
}
