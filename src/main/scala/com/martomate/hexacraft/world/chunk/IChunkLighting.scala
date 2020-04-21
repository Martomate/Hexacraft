package com.martomate.hexacraft.world.chunk

import com.martomate.hexacraft.world.coord.integer.BlockRelChunk
import com.martomate.hexacraft.world.storage.LocalBlockState

trait IChunkLighting {
  def initialized: Boolean
  def init(blocks: Seq[LocalBlockState]): Unit

  def setSunlight(coords: BlockRelChunk, value: Int): Unit
  def getSunlight(coords: BlockRelChunk): Byte
  def setTorchlight(coords: BlockRelChunk, value: Int): Unit
  def getTorchlight(coords: BlockRelChunk): Byte
  def getBrightness(block: BlockRelChunk): Float
}
