package com.martomate.hexacraft.world.gen

import com.flowpowered.nbt.CompoundTag
import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.chunk.{ChunkAddedOrRemovedListener, IChunk}
import com.martomate.hexacraft.world.gen.planner.{SheepPlanner, TreePlanner, WorldFeaturePlanner}
import com.martomate.hexacraft.world.worldlike.BlocksInWorld

class WorldPlanner(world: BlocksInWorld, mainSeed: Long)(implicit cylSize: CylinderSize) extends ChunkAddedOrRemovedListener {
  private val planners: Seq[WorldFeaturePlanner] = Seq(
    new TreePlanner(world, mainSeed),
    new SheepPlanner(world, mainSeed)
  )

  def decorate(chunk: IChunk): Unit = {
    if (!chunk.isDecorated) {
      planners.foreach(_.decorate(chunk))
      chunk.setDecorated()
    }
  }

  override def onChunkAdded(chunk: IChunk): Unit = {
    for (ch <- chunk.coords.extendedNeighbors(1))
      for (p <- planners)
        p.plan(ch)
  }

  override def onChunkRemoved(chunk: IChunk): Unit = ()// same as onChunkAdded, except dropPlan instead of plan. No

}

object WorldPlanner {
  def apply(world: BlocksInWorld, mainSeed: Long, nbt: CompoundTag)(implicit cylSize: CylinderSize): WorldPlanner = {
    val wp = new WorldPlanner(world, mainSeed)

    wp
  }
}
