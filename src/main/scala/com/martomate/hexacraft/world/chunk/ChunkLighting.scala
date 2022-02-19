package com.martomate.hexacraft.world.chunk

import com.martomate.hexacraft.util.SmartArray
import com.martomate.hexacraft.world.block.state.BlockState
import com.martomate.hexacraft.world.chunk.storage.LocalBlockState
import com.martomate.hexacraft.world.coord.integer.BlockRelChunk
import com.martomate.hexacraft.world.lighting.LightPropagator

import scala.collection.mutable

class ChunkLighting(chunk: Chunk, lightPropagator: LightPropagator) {
  private val brightness: SmartArray[Byte] = SmartArray.withByteArray(16*16*16, 0)
  private var brightnessInitialized: Boolean = false

  def initialized: Boolean = brightnessInitialized

  def init(blocks: Array[LocalBlockState]): Unit = {
    if (!brightnessInitialized) {
      brightnessInitialized = true
      val lights = mutable.HashMap.empty[BlockRelChunk, BlockState]

      for (LocalBlockState(c, b) <- blocks) {
        if (b.blockType.lightEmitted != 0) lights(c) = b
      }

      lightPropagator.initBrightnesses(chunk, lights.toMap)
    }
  }

  def setSunlight(coords: BlockRelChunk, value: Int): Unit = {
    brightness(coords.value) = (brightness(coords.value) & 0xf | value << 4).toByte
  }

  def getSunlight(coords: BlockRelChunk): Byte = {
    ((brightness(coords.value) >> 4) & 0xf).toByte
  }

  def setTorchlight(coords: BlockRelChunk, value: Int): Unit = {
    brightness(coords.value) = (brightness(coords.value) & 0xf0 | value).toByte
  }

  def getTorchlight(coords: BlockRelChunk): Byte = {
    (brightness(coords.value) & 0xf).toByte
  }

  def getBrightness(block: BlockRelChunk): Float = {
    math.min((getTorchlight(block) + getSunlight(block)) / 15f, 1.0f)
  }
}
