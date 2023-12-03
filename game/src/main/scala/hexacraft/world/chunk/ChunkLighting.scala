package hexacraft.world.chunk

import hexacraft.util.SmartArray
import hexacraft.world.coord.integer.BlockRelChunk

class ChunkLighting:
  private val brightness: SmartArray[Byte] = SmartArray.withByteArray(16 * 16 * 16, 0)
  private var brightnessInitialized: Boolean = false

  def initialized: Boolean = brightnessInitialized

  def setInitialized(): Unit = brightnessInitialized = true

  def setSunlight(coords: BlockRelChunk, value: Int): Unit =
    brightness(coords.value) = (brightness(coords.value) & 0xf | value << 4).toByte

  def getSunlight(coords: BlockRelChunk): Byte =
    ((brightness(coords.value) >> 4) & 0xf).toByte

  def setTorchlight(coords: BlockRelChunk, value: Int): Unit =
    brightness(coords.value) = (brightness(coords.value) & 0xf0 | value).toByte

  def getTorchlight(coords: BlockRelChunk): Byte =
    (brightness(coords.value) & 0xf).toByte

  def getBrightness(block: BlockRelChunk): Float =
    math.min((getTorchlight(block) + getSunlight(block)) / 15f, 1.0f)
