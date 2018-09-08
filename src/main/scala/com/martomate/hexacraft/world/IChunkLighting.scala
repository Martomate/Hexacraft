package com.martomate.hexacraft.world

import com.martomate.hexacraft.block.BlockState
import com.martomate.hexacraft.world.coord.BlockRelChunk
import com.martomate.hexacraft.world.storage.{IChunk, IWorld, LightPropagator}

import scala.collection.mutable

trait IChunkLighting {
  def initialized: Boolean
  def init(chunk: IChunk, blocks: Seq[(BlockRelChunk, BlockState)]): Unit

  def setSunlight(coords: BlockRelChunk, value: Int): Unit
  def getSunlight(coords: BlockRelChunk): Byte
  def setTorchlight(coords: BlockRelChunk, value: Int): Unit
  def getTorchlight(coords: BlockRelChunk): Byte
  def getBrightness(block: BlockRelChunk): Float
}
