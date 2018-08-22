package com.martomate.hexacraft.world.storage

import com.flowpowered.nbt._
import com.martomate.hexacraft.Camera
import com.martomate.hexacraft.block.{BlockAir, BlockState}
import com.martomate.hexacraft.util.{NBTUtil, TickableTimer, UniquePQ}
import com.martomate.hexacraft.world.Player
import com.martomate.hexacraft.world.coord._
import com.martomate.hexacraft.worldsave.WorldSave
import org.joml.Vector2d

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object World {
  val chunksLoadedPerTick = 2
  val chunkRenderUpdatesPerTick = 1
  val ticksBetweenBlockUpdates = 5
  val ticksBetweenColumnLoading = 5
}

trait BlockSetAndGet {
  def getBlock(coords: BlockRelWorld): BlockState
  def setBlock(coords: BlockRelWorld, block: BlockState): Boolean
  def removeBlock(coords: BlockRelWorld): Boolean
}

trait ChunkAddedOrRemovedListener {
  def onChunkAdded(chunk: Chunk): Unit
  def onChunkRemoved(chunk: Chunk): Unit
}

class World(val worldSettings: WorldSettingsProvider) extends ChunkEventListener with BlockSetAndGet {
  def worldName: String = worldSettings.name
  val size: CylinderSize = worldSettings.size

  val worldGenerator = new WorldGenerator(worldSettings.gen, size)

  val renderDistance: Double = 8 * CoordUtils.y60

  val columns = scala.collection.mutable.Map.empty[Long, ChunkColumn]
  val columnsAtEdge: mutable.Set[ColumnRelWorld] = mutable.Set.empty[ColumnRelWorld]

  private val chunkLoader: ChunkLoader = new ChunkLoader(this)

  private def updateLoadedColumns(): Unit = {
    val rDistSq = math.pow(renderDistance, 2)
    val origin = {
      val temp = chunkLoader.chunkLoadingOrigin.toBlockCoords
      new Vector2d(temp.x / 16, temp.z / 16)
    }
    def inSight(col: ColumnRelWorld): Boolean = {
      col.distSq(origin) <= rDistSq
    }

    val here = ColumnRelWorld(math.floor(origin.x).toInt, math.floor(origin.y).toInt, size)
    ensureColumnExists(here)

    val columnsToAdd = mutable.Set.empty[ColumnRelWorld]
    val columnsToRemove = mutable.Set.empty[ColumnRelWorld]

    def fillInEdgeWithExistingNeighbors(col: ColumnRelWorld): Unit = {
      for (offset <- ChunkColumn.neighbors) {
        val col2 = col.offset(offset._1, offset._2)
        if (columns.contains(col2.value)) {
          columnsToAdd += col2
        }
      }
    }

    def expandEdgeWhereInSightAndReturnSurrounded(col: ColumnRelWorld): Boolean = {
      var surrounded = true
      for (offset <- ChunkColumn.neighbors) {
        val col2 = col.offset(offset._1, offset._2)
        if (inSight(col2)) {
          if (!columns.contains(col2.value)) {
            columnsToAdd += col2
            columns(col2.value) = new ChunkColumn(col2, this)
          }
        } else {
          surrounded = false
        }
      }
      surrounded
    }

    def shouldRemoveColAfterManagingEdgeAt(col: ColumnRelWorld): Boolean = {
      if (!inSight(col)) {
        columns.remove(col.value).get.unload()
        fillInEdgeWithExistingNeighbors(col)
        true
      } else {
        val surrounded = expandEdgeWhereInSightAndReturnSurrounded(col)
        surrounded
      }
    }

    for (col <- columnsAtEdge) {
      val shouldRemoveCol: Boolean = shouldRemoveColAfterManagingEdgeAt(col)

      if (shouldRemoveCol) columnsToRemove += col
    }
    columnsAtEdge ++= columnsToAdd
    columnsAtEdge --= columnsToRemove
  }

  private val blocksToUpdate: UniquePQ[BlockRelWorld] = new UniquePQ(_ => 0, Ordering.by(x => x))

  val player: Player = new Player(this)
  player.fromNBT(worldSettings.playerNBT)

  private[storage] val chunkAddedOrRemovedListeners: ArrayBuffer[ChunkAddedOrRemovedListener] = ArrayBuffer.empty
  def addChunkAddedOrRemovedListener(listener: ChunkAddedOrRemovedListener): Unit = chunkAddedOrRemovedListeners += listener


  def onBlockNeedsUpdate(coords: BlockRelWorld): Unit = blocksToUpdate.enqueue(coords)

  def onChunkNeedsRenderUpdate(coords: ChunkRelWorld): Unit = ()

  def getColumn(coords: ColumnRelWorld): Option[ChunkColumn] = columns.get(coords.value)
  def getChunk(coords: ChunkRelWorld): Option[Chunk]      = getColumn(coords.getColumnRelWorld).flatMap(_.getChunk(coords.getChunkRelColumn))
  def getBlock(coords: BlockRelWorld): BlockState = getColumn(coords.getColumnRelWorld).map(_.getBlock(coords.getBlockRelColumn)).getOrElse(BlockAir.State)
  def setBlock(coords: BlockRelWorld, block: BlockState): Boolean = getChunk(coords.getChunkRelWorld).fold(false)(_.setBlock(coords.getBlockRelChunk, block))
  def removeBlock(coords: BlockRelWorld): Boolean = getChunk(coords.getChunkRelWorld).fold(false)(_.removeBlock(coords.getBlockRelChunk))
  def requestBlockUpdate(coords: BlockRelWorld): Unit = getChunk(coords.getChunkRelWorld).foreach(_.requestBlockUpdate(coords.getBlockRelChunk))

  def getHeight(x: Int, z: Int): Int = {
    val coords = ColumnRelWorld(x >> 4, z >> 4, size)
    ensureColumnExists(coords)
    getColumn(coords).get.heightMap(x & 15, z & 15)
  }

  def tick(camera: Camera): Unit = {
    chunkLoader.tick(camera)

    blockUpdateTimer.tick()
    loadColumnsTimer.tick()

    columns.values.foreach(_.tick())
  }

  private val blockUpdateTimer: TickableTimer = TickableTimer(World.ticksBetweenBlockUpdates) {
    performBlockUpdates()
  }

  private val loadColumnsTimer: TickableTimer = TickableTimer(World.ticksBetweenColumnLoading) {
    updateLoadedColumns()
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
      columns(here.value) = new ChunkColumn(here, this)
      columnsAtEdge += here
    }
  }

  def neighbor(side: Int, chunk: Chunk, coords: BlockRelChunk): (BlockRelChunk, Option[Chunk]) = {
    val (i, j, k) = BlockState.neighborOffsets(side)
    val (i2, j2, k2) = (coords.cx + i, coords.cy + j, coords.cz + k)
    val c2 = BlockRelChunk(i2, j2, k2, coords.cylSize)
    if ((i2 & ~15 | j2 & ~15 | k2 & ~15) == 0) {
      (c2, Some(chunk))
    } else {
      (c2, getChunk(chunk.coords.withBlockCoords(i2, j2, k2).getChunkRelWorld))
    }
  }

  def neighborChunk(chunk: Chunk, side: Int): Option[Chunk] = {
    val (dx, dy, dz) = Chunk.neighborOffsets(side)
    getChunk(chunk.coords.offset(dx, dy, dz))
  }

  def neighborChunks(chunk: Chunk): Iterable[Chunk] = Iterable.tabulate(8)(i => neighborChunk(chunk, i)).flatten

  def getBrightness(block: BlockRelWorld): Float = {
    if (block != null) getChunk(block.getChunkRelWorld).map(_.lighting.getBrightness(block.getBlockRelChunk)).getOrElse(1.0f)
    else 1.0f
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

    worldSettings.saveState(worldTag)

    chunkLoader.unload()
    blockUpdateTimer.active = false
    loadColumnsTimer.active = false
    columns.values.foreach(_.unload())
    columns.clear
  }
}
