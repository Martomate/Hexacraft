package com.martomate.hexacraft.world.loader

import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld

trait ChunkLoadingPrioritizer {
  def +=(chunk: ChunkRelWorld): Unit
  def -=(chunk: ChunkRelWorld): Unit

  def tick(): Unit

  def nextAddableChunk: Option[ChunkRelWorld]
  def nextRemovableChunk: Option[ChunkRelWorld]

  def unload(): Unit
}
