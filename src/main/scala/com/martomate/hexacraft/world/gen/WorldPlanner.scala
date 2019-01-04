package com.martomate.hexacraft.world.gen

import com.flowpowered.nbt.CompoundTag
import com.martomate.hexacraft.util.NBTUtil
import com.martomate.hexacraft.world.chunk.{ChunkAddedOrRemovedListener, IChunk}
import com.martomate.hexacraft.world.gen.planner.{TreePlanner, WorldFeaturePlanner}
import com.martomate.hexacraft.world.worldlike.IWorld

class WorldPlanner(world: IWorld) extends ChunkAddedOrRemovedListener {
  private val planners: Seq[WorldFeaturePlanner] = Seq(
    new TreePlanner
  )

  def decorate(chunk: IChunk): Unit = {
    planners.foreach(_.decorate(chunk, world))
    chunk.setDecorated()
  }

  override def onChunkAdded(chunk: IChunk): Unit = {

  }

  override def onChunkRemoved(chunk: IChunk): Unit = ()

  def toNBT: CompoundTag = NBTUtil.makeCompoundTag("worldPlanner", Seq(

  ))
}

object WorldPlanner {
  def apply(world: IWorld, nbt: CompoundTag): WorldPlanner = {
    val wp = new WorldPlanner(world)

    wp
  }
}
