package com.martomate.hexacraft.world.gen

import com.martomate.hexacraft.world.block.BlockState
import com.martomate.hexacraft.world.chunk.storage.LocalBlockState
import com.martomate.hexacraft.world.coord.integer.{BlockRelWorld, ChunkRelWorld}

import scala.collection.mutable

class PlannedWorldChange:
  private val changes: mutable.Map[ChunkRelWorld, mutable.Buffer[LocalBlockState]] = mutable.Map.empty

  def setBlock(coords: BlockRelWorld, block: BlockState): Unit =
    changes
      .getOrElseUpdate(coords.getChunkRelWorld, mutable.Buffer.empty)
      .append(LocalBlockState(coords.getBlockRelChunk, block))

  def chunkChanges: Map[ChunkRelWorld, Seq[LocalBlockState]] = changes.view.mapValues(_.toSeq).toMap

object PlannedWorldChange:
  def from(blocks: Seq[(BlockRelWorld, BlockState)]): PlannedWorldChange =
    val worldChange = new PlannedWorldChange
    for (c, b) <- blocks do worldChange.setBlock(c, b)
    worldChange
