package com.martomate.hexacraft.world.gen

import com.martomate.hexacraft.world.block.state.BlockState
import com.martomate.hexacraft.world.coord.integer.{BlockRelWorld, ChunkRelWorld}

import scala.collection.mutable

class PlannedWorldChange {
  val changes: mutable.Map[ChunkRelWorld, PlannedChunkChange] = mutable.Map.empty

  def setBlock(coords: BlockRelWorld, block: BlockState): Unit = {
    changes
      .getOrElseUpdate(coords.getChunkRelWorld, new PlannedChunkChange)
      .setBlock(coords.getBlockRelChunk, block)
  }

  def chunkChanges: Map[ChunkRelWorld, PlannedChunkChange] = changes.toMap
}
