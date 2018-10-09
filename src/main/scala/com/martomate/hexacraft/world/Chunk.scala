package com.martomate.hexacraft.world

import com.martomate.hexacraft.util.NBTUtil
import com.martomate.hexacraft.world.block.HexBox
import com.martomate.hexacraft.world.block.state.BlockState
import com.martomate.hexacraft.world.chunk._
import com.martomate.hexacraft.world.coord.fp.{BlockCoords, CylCoords}
import com.martomate.hexacraft.world.coord.integer.{BlockRelChunk, BlockRelWorld, ChunkRelWorld}
import com.martomate.hexacraft.world.entity._
import com.martomate.hexacraft.world.lighting.{ChunkLighting, LightPropagator}
import com.martomate.hexacraft.world.storage.{ChunkData, ChunkStorage}

import scala.collection.mutable.ArrayBuffer

class Chunk(val coords: ChunkRelWorld, generator: IChunkGenerator, lightPropagator: LightPropagator) extends IChunk {
  import coords.cylSize.impl

  private val chunkData: ChunkData = generator.loadData()

  private def storage: ChunkStorage = chunkData.storage
  private var needsToSave = false

  private val eventListeners: ArrayBuffer[ChunkEventListener] = ArrayBuffer.empty
  def addEventListener(listener: ChunkEventListener): Unit = eventListeners += listener
  def removeEventListener(listener: ChunkEventListener): Unit = eventListeners -= listener

  private val blockEventListeners: ArrayBuffer[ChunkBlockListener] = ArrayBuffer.empty
  def addBlockEventListener(listener: ChunkBlockListener): Unit = blockEventListeners += listener
  def removeBlockEventListener(listener: ChunkBlockListener): Unit = blockEventListeners -= listener

  val lighting: IChunkLighting = new ChunkLighting(this, lightPropagator)
  override val entities: EntitiesInChunk = new EntitiesInChunkImpl
  // TODO: temporary
  entities += new TempEntity(BlockCoords(BlockRelWorld(0, 0, 0, coords)).toCylCoords, new TempEntityModel(BlockCoords(BlockRelWorld(0, 0, 0)).toCylCoords, new HexBox(0.5f, 0, 0.5f)))

  def init(): Unit = {
    requestRenderUpdate()
    requestRenderUpdateForAllNeighbors()
  }

  def blocks: ChunkStorage = storage

  def getBlock(coords: BlockRelChunk): BlockState = storage.getBlock(coords)

  def setBlock(blockCoords: BlockRelChunk, block: BlockState): Boolean = {
    val before = getBlock(blockCoords)
    if (before != block) {
      storage.setBlock(blockCoords, block)
      needsToSave = true

      for (listener <- blockEventListeners) {
        listener.onSetBlock(BlockRelWorld(blockCoords, coords), before, block)
      }

      handleLightingOnSetBlock(blockCoords, block)
    }
    true
  }

  def removeBlock(coords: BlockRelChunk): Boolean = setBlock(coords, BlockState.Air)

  private def handleLightingOnSetBlock(blockCoords: BlockRelChunk, block: BlockState): Unit = {
    lightPropagator.removeTorchlight(this, blockCoords)
    lightPropagator.removeSunlight(this, blockCoords)
    if (block.blockType.lightEmitted != 0) {
      lightPropagator.addTorchlight(this, blockCoords, block.blockType.lightEmitted)
    }
  }

  def requestBlockUpdate(coords: BlockRelChunk): Unit = eventListeners.foreach(_.onBlockNeedsUpdate(BlockRelWorld(coords, this.coords)))

  def requestRenderUpdate(): Unit = eventListeners.foreach(_.onChunkNeedsRenderUpdate(coords))

  private def requestRenderUpdateForAllNeighbors(): Unit =
    for (side <- 0 until 8)
      eventListeners.foreach(_.onChunksNeighborNeedsRenderUpdate(coords, side))

  def tick(): Unit = {
    chunkData.optimizeStorage()

    // TODO: temp
    entities.allEntities foreach {
      case temp: TempEntity =>
        temp.tempTick()
      case _ =>
    }
  }

  def isEmpty: Boolean = storage.numBlocks == 0

  def unload(): Unit = {
    if (needsToSave) {
      val chunkTag = NBTUtil.makeCompoundTag("chunk", storage.toNBT)// Add more tags with ++
      generator.saveData(chunkTag)
    }

    requestRenderUpdateForAllNeighbors()
  }
}
