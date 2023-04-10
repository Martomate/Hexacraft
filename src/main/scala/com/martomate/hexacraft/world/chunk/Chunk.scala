package com.martomate.hexacraft.world.chunk

import com.martomate.hexacraft.util.{CylinderSize, EventDispatcher, NBTUtil, RevokeTrackerFn, Tracker}
import com.martomate.hexacraft.world.{BlocksInWorld, CollisionDetector, LightPropagator, WorldProvider}
import com.martomate.hexacraft.world.block.{Block, Blocks, BlockState}
import com.martomate.hexacraft.world.chunk.storage.ChunkStorage
import com.martomate.hexacraft.world.coord.integer.{BlockRelChunk, BlockRelWorld, ChunkRelWorld}
import com.martomate.hexacraft.world.entity.{Entity, EntityRegistry}
import com.martomate.hexacraft.world.gen.WorldGenerator

import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer

object Chunk:
  def apply(
      coords: ChunkRelWorld,
      world: BlocksInWorld,
      worldProvider: WorldProvider,
      entityRegistry: EntityRegistry = EntityRegistry.empty
  )(using CylinderSize, Blocks): Chunk =
    val worldGenerator = new WorldGenerator(worldProvider.getWorldInfo.gen)
    val chunkGenerator = new ChunkGenerator(coords, world, worldProvider, worldGenerator, entityRegistry)
    new Chunk(coords, chunkGenerator, new LightPropagator(world))

  enum Event:
    case BlockNeedsUpdate(coords: BlockRelWorld)
    case ChunkNeedsRenderUpdate(coords: ChunkRelWorld)
    case BlockReplaced(coords: BlockRelWorld, prev: BlockState, now: BlockState)

class Chunk(val coords: ChunkRelWorld, generator: ChunkGenerator, lightPropagator: LightPropagator):
  private val chunkData: ChunkData = generator.loadData()

  private def storage: ChunkStorage = chunkData.storage
  private var needsToSave = false

  private val dispatcher = new EventDispatcher[Chunk.Event]
  def trackEvents(tracker: Tracker[Chunk.Event]): RevokeTrackerFn = dispatcher.track(tracker)

  val lighting: ChunkLighting = new ChunkLighting(lightPropagator)
  def entities: EntitiesInChunk = chunkData.entities

  def addEntity(entity: Entity): Unit = entities += entity
  def removeEntity(entity: Entity): Unit = entities -= entity

  def blocks: ChunkStorage = storage

  def getBlock(coords: BlockRelChunk): BlockState = storage.getBlock(coords)

  def setBlock(blockCoords: BlockRelChunk, block: BlockState): Unit =
    val before = getBlock(blockCoords)
    if before != block then
      storage.setBlock(blockCoords, block)
      needsToSave = true

      dispatcher.notify(Chunk.Event.BlockReplaced(BlockRelWorld.fromChunk(blockCoords, coords), before, block))

      handleLightingOnSetBlock(blockCoords, block)

  def removeBlock(coords: BlockRelChunk): Unit = setBlock(coords, BlockState.Air)

  private def handleLightingOnSetBlock(blockCoords: BlockRelChunk, block: BlockState): Unit =
    lightPropagator.removeTorchlight(this, blockCoords)
    lightPropagator.removeSunlight(this, blockCoords)
    if block.blockType.lightEmitted != 0 then
      lightPropagator.addTorchlight(this, blockCoords, block.blockType.lightEmitted)

  def requestBlockUpdate(coords: BlockRelChunk): Unit =
    dispatcher.notify(Chunk.Event.BlockNeedsUpdate(BlockRelWorld.fromChunk(coords, this.coords)))

  def requestRenderUpdate(): Unit =
    dispatcher.notify(Chunk.Event.ChunkNeedsRenderUpdate(coords))

  def tick(world: BlocksInWorld, collisionDetector: CollisionDetector): Unit =
    chunkData.optimizeStorage()

    tickEntities(entities.allEntities)

    @tailrec
    def tickEntities(ents: Iterable[Entity]): Unit =
      if ents.nonEmpty then
        ents.head.tick(world, collisionDetector)
        tickEntities(ents.tail)

  def saveIfNeeded(): Unit = if needsToSave || entities.needsToSave then save()

  def unload(): Unit =
    saveIfNeeded()

  private def save(): Unit =
    val chunkTag = NBTUtil.makeCompoundTag("chunk", chunkData.toNBT) // Add more tags with ++
    generator.saveData(chunkTag)
    needsToSave = false

  def isDecorated: Boolean = chunkData.isDecorated
  def setDecorated(): Unit =
    if !chunkData.isDecorated then
      chunkData.isDecorated = true
      needsToSave = true
      save()
