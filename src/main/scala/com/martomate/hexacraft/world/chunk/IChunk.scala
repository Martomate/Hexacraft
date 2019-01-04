package com.martomate.hexacraft.world.chunk

import com.martomate.hexacraft.world.block.state.BlockState
import com.martomate.hexacraft.world.coord.integer.{BlockRelChunk, ChunkRelWorld}
import com.martomate.hexacraft.world.storage.ChunkStorage

trait IChunk {
  def isDecorated: Boolean
  def setDecorated(): Unit

  val coords: ChunkRelWorld
  def lighting: IChunkLighting

  def init(): Unit
  def tick(): Unit

  def isEmpty: Boolean
  def blocks: ChunkStorage
  def entities: EntitiesInChunk
  def getBlock(coords: BlockRelChunk): BlockState
  def setBlock(blockCoords: BlockRelChunk, block: BlockState): Boolean
  def removeBlock(coords: BlockRelChunk): Boolean

  def addEventListener(listener: ChunkEventListener): Unit
  def removeEventListener(listener: ChunkEventListener): Unit

  def addBlockEventListener(listener: ChunkBlockListener): Unit
  def removeBlockEventListener(listener: ChunkBlockListener): Unit

  def requestRenderUpdate(): Unit
  def requestBlockUpdate(coords: BlockRelChunk): Unit

  def unload(): Unit
}
