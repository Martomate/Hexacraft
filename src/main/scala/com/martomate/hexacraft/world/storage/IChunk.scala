package com.martomate.hexacraft.world.storage

import com.martomate.hexacraft.block.BlockState
import com.martomate.hexacraft.world.IChunkLighting
import com.martomate.hexacraft.world.coord.{BlockRelChunk, ChunkRelWorld}

trait IChunk {
  def coords: ChunkRelWorld
  def lighting: IChunkLighting

  def init(): Unit
  def tick(): Unit

  def isEmpty: Boolean
  def blocks: ChunkStorage
  def getBlock(coords: BlockRelChunk): BlockState
  def setBlock(blockCoords: BlockRelChunk, block: BlockState): Boolean
  def removeBlock(coords: BlockRelChunk): Boolean

  def addEventListener(listener: ChunkEventListener): Unit
  def removeEventListener(listener: ChunkEventListener): Unit

  def addBlockEventListener(listener: ChunkBlockListener): Unit
  def removeBlockEventListener(listener: ChunkBlockListener): Unit

  def requestRenderUpdate(): Unit
  def requestBlockUpdate(coords: BlockRelChunk): Unit
  def doBlockUpdate(coords: BlockRelChunk): Unit

  def unload(): Unit
}

