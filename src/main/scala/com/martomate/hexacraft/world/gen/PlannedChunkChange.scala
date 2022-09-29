package com.martomate.hexacraft.world.gen

import com.martomate.hexacraft.world.block.BlockState
import com.martomate.hexacraft.world.chunk.Chunk
import com.martomate.hexacraft.world.coord.integer.BlockRelChunk

import scala.collection.mutable

class PlannedChunkChange {
  val changes: mutable.Buffer[(BlockRelChunk, BlockState)] = mutable.Buffer.empty

  def merge(ch: PlannedChunkChange): Unit = changes ++= ch.changes

  def setBlock(coords: BlockRelChunk, block: BlockState): Unit = changes += coords -> block

  def applyChanges(chunk: Chunk): Unit = {
    for ((c, b) <- changes) {
      chunk.setBlock(c, b)
    }
  }
}
