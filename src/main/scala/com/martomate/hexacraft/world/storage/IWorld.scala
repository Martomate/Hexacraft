package com.martomate.hexacraft.world.storage

import com.flowpowered.nbt._
import com.martomate.hexacraft.Camera
import com.martomate.hexacraft.block.{BlockAir, BlockState}
import com.martomate.hexacraft.util.{NBTUtil, TickableTimer, UniquePQ}
import com.martomate.hexacraft.world.{CylinderSize, Player, PosAndDir}
import com.martomate.hexacraft.world.coord._
import com.martomate.hexacraft.world.render.LightPropagator
import com.martomate.hexacraft.worldsave.WorldSave

import scala.collection.mutable.ArrayBuffer

object IWorld {
  val ticksBetweenBlockUpdates = 5
}

trait IWorld extends ChunkEventListener with BlockSetAndGet with BlocksInWorld {
  def size: CylinderSize
  def worldSettings: WorldSettingsProvider
  def worldGenerator: WorldGenerator
  def renderDistance: Double

  def getHeight(x: Int, z: Int): Int

  private[storage] def chunkAddedOrRemovedListeners: Iterable[ChunkAddedOrRemovedListener]
  def addChunkAddedOrRemovedListener(listener: ChunkAddedOrRemovedListener): Unit
}

class World(val worldSettings: WorldSettingsProvider) extends IWorld {
  def worldName: String = worldSettings.name
  val size: CylinderSize = worldSettings.size

  val worldGenerator = new WorldGenerator(worldSettings.gen, size)
  private val lightPropagator: LightPropagator = new LightPropagator(this)

  val renderDistance: Double = 8 * CylinderSize.y60

  private val columns = scala.collection.mutable.Map.empty[Long, ChunkColumn]

  private val chunkLoadingOrigin = new PosAndDir(size)
  private val chunkLoader: ChunkLoader = new ChunkLoaderWithOrigin(
    size,
    renderDistance,
    columns,
    coords => new ChunkColumn(coords, this),
    coords => new Chunk(coords, new ChunkGenerator(coords, this), this, lightPropagator),
    chunkLoadingOrigin
  )

  private val blocksToUpdate: UniquePQ[BlockRelWorld] = new UniquePQ(_ => 0, Ordering.by(x => x))

  val player: Player = new Player(this)
  player.fromNBT(worldSettings.playerNBT)

  private[storage] val chunkAddedOrRemovedListeners: ArrayBuffer[ChunkAddedOrRemovedListener] = ArrayBuffer.empty
  def addChunkAddedOrRemovedListener(listener: ChunkAddedOrRemovedListener): Unit = chunkAddedOrRemovedListeners += listener

  addChunkAddedOrRemovedListener(chunkLoader)

  def onBlockNeedsUpdate(coords: BlockRelWorld): Unit = blocksToUpdate.enqueue(coords)

  def onChunkNeedsRenderUpdate(coords: ChunkRelWorld): Unit = ()

  def getColumn(coords: ColumnRelWorld): Option[ChunkColumn] = columns.get(coords.value)
  def getChunk(coords: ChunkRelWorld): Option[Chunk] = getColumn(coords.getColumnRelWorld).flatMap(_.getChunk(coords.getChunkRelColumn))
  def getBlock(coords: BlockRelWorld): BlockState    = getColumn(coords.getColumnRelWorld).map(_.getBlock(coords.getBlockRelColumn)).getOrElse(BlockAir.State)
  def setBlock(coords: BlockRelWorld, block: BlockState): Boolean = getChunk(coords.getChunkRelWorld).fold(false)(_.setBlock(coords.getBlockRelChunk, block))
  def removeBlock(coords: BlockRelWorld): Boolean = getChunk(coords.getChunkRelWorld).fold(false)(_.removeBlock(coords.getBlockRelChunk))
  def requestBlockUpdate(coords: BlockRelWorld): Unit = getChunk(coords.getChunkRelWorld).foreach(_.requestBlockUpdate(coords.getBlockRelChunk))

  def getHeight(x: Int, z: Int): Int = {
    val coords = ColumnRelWorld(x >> 4, z >> 4, size)
    ensureColumnExists(coords)
    getColumn(coords).get.heightMap(x & 15, z & 15)
  }

  def tick(camera: Camera): Unit = {
    chunkLoadingOrigin.setPosAndDirFrom(camera)
    chunkLoader.tick()
    for (ch <- chunkLoader.chunksToAdd()) {
      ensureColumnExists(ch.coords.getColumnRelWorld)
      getColumn(ch.coords.getColumnRelWorld).foreach(_.setChunk(ch))
      chunkAddedOrRemovedListeners.foreach(_.onChunkAdded(ch))
      ch.init()
    }

    for (ch <- chunkLoader.chunksToRemove()) {
      getColumn(ch.getColumnRelWorld) foreach { col =>
        col.removeChunk(ch.getChunkRelColumn).foreach { c =>
          chunkAddedOrRemovedListeners.foreach(_.onChunkRemoved(c))
          c.unload()
        }
        if (col.isEmpty) columns.remove(col.coords.value).foreach { c =>
          c.unload()
          chunkLoader.onColumnRemoved(c)
        }
      }
    }

    blockUpdateTimer.tick()

    columns.values.foreach(_.tick())
  }

  private val blockUpdateTimer: TickableTimer = TickableTimer(IWorld.ticksBetweenBlockUpdates) {
    performBlockUpdates()
  }

  private def performBlockUpdates(): Unit = {
    val blocksToUpdateLen = blocksToUpdate.size
    for (_ <- 0 until blocksToUpdateLen) {
      val c = blocksToUpdate.dequeue()
      getChunk(c.getChunkRelWorld).foreach(_.doBlockUpdate(c.getBlockRelChunk))
    }
  }

  private def ensureColumnExists(here: ColumnRelWorld): Unit = {
    if (!columns.contains(here.value)) {
      val col = new ChunkColumn(here, this)
      columns(here.value) = col
      chunkLoader.onColumnAdded(col)
    }
  }

  def getBrightness(block: BlockRelWorld): Float = {
    if (block != null) getChunk(block.getChunkRelWorld).map(_.lighting.getBrightness(block.getBlockRelChunk)).getOrElse(1.0f)
    else 1.0f
  }

  def onReloadedResources(): Unit = {
    columns.values.foreach(_.onReloadedResources())
  }

  def unload(): Unit = {
    val worldTag = NBTUtil.makeCompoundTag("world", Seq(
      new ShortTag("version", WorldSave.LatestVersion),
      NBTUtil.makeCompoundTag("general", Seq(
        new ByteTag("worldSize", size.worldSize.toByte),
        new StringTag("name", worldName)
      )),
      worldGenerator.toNBT,
      player.toNBT
    ))

    worldSettings.saveState(worldTag, "world.dat")

    chunkLoader.unload()
    blockUpdateTimer.active = false
    columns.values.foreach(_.unload())
    columns.clear
  }

  override def onChunksNeighborNeedsRenderUpdate(coords: ChunkRelWorld, side: Int): Unit = ()//neighborChunk(coords, side).foreach(_.requestRenderUpdate())
}
