package com.martomate.hexacraft.world.storage

import com.martomate.hexacraft.block.{Block, BlockAir, BlockState}
import com.martomate.hexacraft.util.{NBTUtil, PreparableRunnerWithIndex}
import com.martomate.hexacraft.world.ChunkLighting
import com.martomate.hexacraft.world.coord.{BlockRelChunk, BlockRelWorld, ChunkRelWorld}
import com.martomate.hexacraft.world.render.{ChunkRenderer, LightPropagator}

import scala.collection.mutable.ArrayBuffer

object Chunk {
  val neighborOffsets: Seq[(Int, Int, Int)] = Seq(
    (0, 1, 0),
    (1, 0, 0),
    (0, 0, 1),
    (-1, 0, 1),
    (0, -1, 0),
    (-1, 0, 0),
    (0, 0, -1),
    (1, 0, -1))
}

trait ChunkEventListener {
  def onBlockNeedsUpdate(coords: BlockRelWorld): Unit
  def onChunkNeedsRenderUpdate(coords: ChunkRelWorld): Unit
}

class Chunk(val coords: ChunkRelWorld, generator: ChunkGenerator, world: World) {
  private def neighbors: Iterable[Chunk] = Option(world).map(_.neighborChunks(this)).getOrElse(Iterable.empty)
  private def neighborChunk(side: Int): Option[Chunk] = Option(world).flatMap(_.neighborChunk(this, side))

  neighbors.foreach(_.requestRenderUpdate())

  private val chunkData: ChunkData = generator.loadData()

  private def storage: ChunkStorage = chunkData.storage
  private var needsToSave = false

  private val eventListeners: ArrayBuffer[ChunkEventListener] = ArrayBuffer.empty
  def addEventListener(listener: ChunkEventListener): Unit = eventListeners += listener
  def removeEventListener(listener: ChunkEventListener): Unit = eventListeners -= listener
  addEventListener(world)

  val renderer: ChunkRenderer = new ChunkRenderer(this, world)
  val lighting: ChunkLighting = new ChunkLighting

  private val needsBlockUpdateToggle = new PreparableRunnerWithIndex[BlockRelChunk](_.value)(
    coords => eventListeners.foreach(_.onBlockNeedsUpdate(coords.withChunk(this.coords))),
    coords => getBlock(coords).blockType.doUpdate(coords.withChunk(this.coords), world)
  )

  column.onChunkLoaded(this)
  requestRenderUpdate()

  def blocks: ChunkStorage = storage
  def column: ChunkColumn = world.getColumn(coords.getColumnRelWorld).get

  def getBlock(coords: BlockRelChunk): BlockState = storage.getBlock(coords)

  def setBlock(blockCoords: BlockRelChunk, block: BlockState): Boolean = {
    val before = getBlock(blockCoords)
    storage.setBlock(blockCoords, block)
    if (before.blockType == Block.Air || before != block) {
      onBlockModified(blockCoords)
    }
    LightPropagator.removeTorchlight(world, this, blockCoords)
    LightPropagator.removeSunlight(world, this, blockCoords)
    if (block.blockType.lightEmitted != 0) {
      LightPropagator.addTorchlight(world, this, blockCoords, block.blockType.lightEmitted)
    }
    column.onSetBlock(blockCoords.withChunk(coords.getChunkRelColumn), block)
    true
  }

  def removeBlock(coords: BlockRelChunk): Boolean = {
    storage.removeBlock(coords)
    onBlockModified(coords)
    LightPropagator.removeTorchlight(world, this, coords)
    LightPropagator.updateSunlight(world, this, coords)
    column.onSetBlock(coords.withChunk(this.coords.getChunkRelColumn), BlockAir.State)
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
      val off = Chunk.neighborOffsets(i)
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

  def tick(): Unit = {
    chunkData.optimizeStorage()
  }

  def isEmpty: Boolean = storage.numBlocks == 0

  def unload(): Unit = {
    if (needsToSave) {
      val chunkTag = NBTUtil.makeCompoundTag("chunk", storage.toNBT)// Add more tags with ++
      generator.saveData(chunkTag)
    }
    
    neighbors.foreach(_.requestRenderUpdate())
    removeEventListener(world)
  }
}
