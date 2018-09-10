package com.martomate.hexacraft.world.storage

import com.martomate.hexacraft.block.{BlockAir, BlockState, Blocks}
import com.martomate.hexacraft.util.{NBTUtil, PreparableRunnerWithIndex}
import com.martomate.hexacraft.world.coord.integer.{BlockRelChunk, BlockRelWorld, ChunkRelWorld}
import com.martomate.hexacraft.world.{ChunkLighting, IChunkLighting}

import scala.collection.mutable.ArrayBuffer

class Chunk(val coords: ChunkRelWorld, generator: ChunkGenerator, world: IWorld, lightPropagator: LightPropagator) extends IChunk {
  private def neighborChunk(side: Int): Option[IChunk] = Option(world).flatMap(_.neighborChunk(coords, side))

  private val chunkData: ChunkData = generator.loadData()

  private def storage: ChunkStorage = chunkData.storage
  private var needsToSave = false

  private val eventListeners: ArrayBuffer[ChunkEventListener] = ArrayBuffer.empty
  def addEventListener(listener: ChunkEventListener): Unit = eventListeners += listener
  def removeEventListener(listener: ChunkEventListener): Unit = eventListeners -= listener

  private val blockEventListeners: ArrayBuffer[ChunkBlockListener] = ArrayBuffer.empty
  def addBlockEventListener(listener: ChunkBlockListener): Unit = blockEventListeners += listener
  def removeBlockEventListener(listener: ChunkBlockListener): Unit = blockEventListeners -= listener

  val lighting: IChunkLighting = new ChunkLighting(lightPropagator)

  private val needsBlockUpdateToggle = new PreparableRunnerWithIndex[BlockRelChunk](_.value)(
    coords => eventListeners.foreach(_.onBlockNeedsUpdate(BlockRelWorld(coords, this.coords))),
    coords => getBlock(coords).blockType.doUpdate(BlockRelWorld(coords, this.coords), world)
  )

  def init(): Unit = {
    requestRenderUpdate()
    requestRenderUpdateForAllNeighbors()
  }

  def blocks: ChunkStorage = storage

  def getBlock(coords: BlockRelChunk): BlockState = storage.getBlock(coords)

  def setBlock(blockCoords: BlockRelChunk, block: BlockState): Boolean = {
    val before = getBlock(blockCoords)
    storage.setBlock(blockCoords, block)
    if (before.blockType == Blocks.Air || before != block) {
      onBlockModified(blockCoords)
    }
    lightPropagator.removeTorchlight(this, blockCoords)
    lightPropagator.removeSunlight(this, blockCoords)
    if (block.blockType.lightEmitted != 0) {
      lightPropagator.addTorchlight(this, blockCoords, block.blockType.lightEmitted)
    }
    blockEventListeners.foreach(_.onSetBlock(BlockRelWorld(blockCoords, coords), before, block))
    true
  }

  def removeBlock(coords: BlockRelChunk): Boolean = {
    val before = getBlock(coords)
    storage.removeBlock(coords)
    onBlockModified(coords)
    lightPropagator.removeTorchlight(this, coords)
    lightPropagator.updateSunlight(this, coords)
    blockEventListeners.foreach(_.onSetBlock(BlockRelWorld(coords, this.coords), before, BlockAir.State))
    true
  }

  private def onBlockModified(coords: BlockRelChunk): Unit = {
    def affectableChunkOffset(where: Byte): Int = if (where == 0) -1 else if (where == 15) 1 else 0

    def isInNeighborChunk(chunkOffset: (Int, Int, Int)) = {
      val xx = affectableChunkOffset(coords.cx)
      val yy = affectableChunkOffset(coords.cy)
      val zz = affectableChunkOffset(coords.cz)

      chunkOffset._1 * xx == 1 || chunkOffset._2 * yy == 1 || chunkOffset._3 * zz == 1
    }

    def offsetCoords(c: BlockRelChunk, off: (Int, Int, Int)) = c.offset(off._1, off._2, off._3)


    requestRenderUpdate()

    for (i <- 0 until 8) {
      val off = ChunkRelWorld.neighborOffsets(i)
      val c2 = offsetCoords(coords, off)
      if (isInNeighborChunk(off)) {
        neighborChunk(i).foreach(n => {
          n.requestRenderUpdate()
          n.requestBlockUpdate(c2)
        })
      }
      else requestBlockUpdate(c2)
    }

    requestBlockUpdate(coords)
    needsToSave = true
  }

  def requestBlockUpdate(coords: BlockRelChunk): Unit = needsBlockUpdateToggle.prepare(coords)
  def doBlockUpdate(coords: BlockRelChunk): Unit = needsBlockUpdateToggle.activate(coords)

  def requestRenderUpdate(): Unit = eventListeners.foreach(_.onChunkNeedsRenderUpdate(coords))

  private def requestRenderUpdateForAllNeighbors(): Unit =
    for (side <- 0 until 8)
      eventListeners.foreach(_.onChunksNeighborNeedsRenderUpdate(coords, side))

  private def requestRenderUpdateForNeighbor(side: Int): Unit =
    eventListeners.foreach(_.onChunksNeighborNeedsRenderUpdate(coords, side))

  def tick(): Unit = {
    chunkData.optimizeStorage()
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
