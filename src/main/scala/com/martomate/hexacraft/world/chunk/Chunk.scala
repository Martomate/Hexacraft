package com.martomate.hexacraft.world.chunk

import com.martomate.hexacraft.util.{CylinderSize, NBTUtil}
import com.martomate.hexacraft.world.{BlocksInWorld, CollisionDetector, LightPropagator, WorldProvider}
import com.martomate.hexacraft.world.block.{Block, BlockState, Blocks}
import com.martomate.hexacraft.world.chunk.storage.ChunkStorage
import com.martomate.hexacraft.world.coord.integer.{BlockRelChunk, BlockRelWorld, ChunkRelWorld}
import com.martomate.hexacraft.world.entity.{Entity, EntityModelLoader, EntityRegistry}
import com.martomate.hexacraft.world.gen.WorldGenerator

import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer

object Chunk {
  def apply(coords: ChunkRelWorld, world: BlocksInWorld, worldProvider: WorldProvider)(using
      EntityModelLoader,
      CylinderSize,
      Blocks
  ): Chunk = {
    val worldGenerator = new WorldGenerator(worldProvider.getWorldInfo.gen)
    val entityRegistry = EntityRegistry.empty
    val chunkGenerator =
      new ChunkGenerator(coords, world, worldProvider, worldGenerator, entityRegistry)
    new Chunk(coords, chunkGenerator, new LightPropagator(world))
  }
}

class Chunk(val coords: ChunkRelWorld, generator: ChunkGenerator, lightPropagator: LightPropagator)
    extends BlockInChunkAccessor {
  private val chunkData: ChunkData = generator.loadData()

  private def storage: ChunkStorage = chunkData.storage
  private var needsToSave = false

  private val eventListeners: ArrayBuffer[ChunkEventListener] = ArrayBuffer.empty
  def addEventListener(listener: ChunkEventListener): Unit = eventListeners += listener
  def removeEventListener(listener: ChunkEventListener): Unit = eventListeners -= listener

  private val blockEventListeners: ArrayBuffer[ChunkBlockListener] = ArrayBuffer.empty
  def addBlockEventListener(listener: ChunkBlockListener): Unit = blockEventListeners += listener
  def removeBlockEventListener(listener: ChunkBlockListener): Unit = blockEventListeners -= listener

  val lighting: ChunkLighting = new ChunkLighting(this, lightPropagator)
  def entities: EntitiesInChunk = chunkData.entities

  def init(): Unit = {
    requestRenderUpdate()
    requestRenderUpdateForAllNeighbors()
  }

  def blocks: ChunkStorage = storage

  def getBlock(coords: BlockRelChunk): BlockState = storage.getBlock(coords)

  def mapBlock[T](coords: BlockRelChunk, func: (Block, Byte) => T): T =
    storage.mapBlock(coords, func)

  def setBlock(blockCoords: BlockRelChunk, block: BlockState): Unit = {
    val before = getBlock(blockCoords)
    if (before != block) {
      storage.setBlock(blockCoords, block)
      needsToSave = true

      for (listener <- blockEventListeners) {
        listener.onSetBlock(BlockRelWorld.fromChunk(blockCoords, coords), before, block)
      }

      handleLightingOnSetBlock(blockCoords, block)
    }
  }

  def removeBlock(coords: BlockRelChunk): Unit = setBlock(coords, BlockState.Air)

  private def handleLightingOnSetBlock(blockCoords: BlockRelChunk, block: BlockState): Unit = {
    lightPropagator.removeTorchlight(this, blockCoords)
    lightPropagator.removeSunlight(this, blockCoords)
    if (block.blockType.lightEmitted != 0) {
      lightPropagator.addTorchlight(this, blockCoords, block.blockType.lightEmitted)
    }
  }

  def requestBlockUpdate(coords: BlockRelChunk): Unit =
    eventListeners.foreach(_.onBlockNeedsUpdate(BlockRelWorld.fromChunk(coords, this.coords)))

  def requestRenderUpdate(): Unit = eventListeners.foreach(_.onChunkNeedsRenderUpdate(coords))

  private def requestRenderUpdateForAllNeighbors(): Unit =
    for (side <- 0 until 8)
      eventListeners.foreach(_.onChunksNeighborNeedsRenderUpdate(coords, side))

  def tick(world: BlocksInWorld, collisionDetector: CollisionDetector): Unit = {
    chunkData.optimizeStorage()

    tickEntities(entities.allEntities)

    @tailrec
    def tickEntities(ents: Iterable[Entity]): Unit = {
      if (ents.nonEmpty) {
        ents.head.tick(world, collisionDetector)
        tickEntities(ents.tail)
      }
    }
  }

  def hasNoBlocks: Boolean = storage.numBlocks == 0

  def saveIfNeeded(): Unit = {
    if (needsToSave || entities.needsToSave) {
      save()
    }
  }

  def unload(): Unit = {
    saveIfNeeded()

    requestRenderUpdateForAllNeighbors()

    blockEventListeners.clear()
    eventListeners.clear()
  }

  private def save(): Unit = {
    val chunkTag = NBTUtil.makeCompoundTag("chunk", chunkData.toNBT) // Add more tags with ++
    generator.saveData(chunkTag)
    needsToSave = false
  }

  def isDecorated: Boolean = chunkData.isDecorated
  def setDecorated(): Unit = {
    if (!chunkData.isDecorated) {
      chunkData.isDecorated = true
      needsToSave = true
      save()
    }
  }
}
