package com.martomate.hexacraft.world.chunk

import com.martomate.hexacraft.world.coord.integer.{BlockRelChunk, ChunkRelWorld}
import com.martomate.hexacraft.world.storage.ChunkStorage

trait IChunk extends BlockInChunkAccessor {
  def isDecorated: Boolean
  def setDecorated(): Unit

  val coords: ChunkRelWorld
  def lighting: IChunkLighting

  def init(): Unit
  def tick(): Unit

  def hasNoBlocks: Boolean
  def blocks: ChunkStorage
  def entities: EntitiesInChunk

  def addEventListener(listener: ChunkEventListener): Unit
  def removeEventListener(listener: ChunkEventListener): Unit

  def addBlockEventListener(listener: ChunkBlockListener): Unit
  def removeBlockEventListener(listener: ChunkBlockListener): Unit

  def requestRenderUpdate(): Unit
  def requestBlockUpdate(coords: BlockRelChunk): Unit

  def saveIfNeeded(): Unit

  def unload(): Unit
}
