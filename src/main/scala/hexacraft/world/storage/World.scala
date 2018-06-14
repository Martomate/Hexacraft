package hexacraft.world.storage

import com.flowpowered.nbt._
import hexacraft.Camera
import hexacraft.block.{BlockAir, BlockState}
import hexacraft.util.{NBTUtil, TickableTimer}
import hexacraft.world.Player
import hexacraft.world.coord._
import org.joml.{Vector2d, Vector3d, Vector4d}

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, Set => MutableSet}

object World {
  val chunksLoadedPerTick = 2
  val chunkRenderUpdatesPerTick = 2
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

  private[storage] var chunkLoadingOrigin: CylCoords = _
  private[storage] val chunkLoadingDirection: Vector3d = new Vector3d()
  private val chunksToLoad = mutable.PriorityQueue.empty[(Double, ChunkRelWorld)](Ordering.by(-_._1))

  def addChunkToLoadingQueue(coords: ChunkRelWorld): Unit = {
    chunksToLoad.enqueue(makeChunkToLoadTuple(coords))
  }

  private def makeChunkToLoadTuple(coords: ChunkRelWorld): (Double, ChunkRelWorld) = {
    val corners = for {
      i <- 0 to 1
      j <- 0 to 1
      k <- 0 to 1
    } yield (15 * i, 15 * j, 15 * k)
    val dist = ((corners :+ (8, 8, 8)) map {
      t =>
        val cyl = BlockCoords(coords.withBlockCoords(t._1, t._2, t._3), size).toCylCoords
        val cDir = cyl.toNormalCoords(chunkLoadingOrigin).toVector3d.normalize()
        val dot = chunkLoadingDirection.dot(cDir)
        chunkLoadingOrigin.distanceSq(cyl) * (1.25 - math.pow((dot + 1) / 2, 4)) / 1.25
    }).min

    (dist, coords)
  }

  private def setChunkLoadingCenterAndDirection(camera: Camera): Vector3d = {
    chunkLoadingOrigin = CylCoords(player.position.x, player.position.y, player.position.z, size)
    val vec4 = new Vector4d(0, 0, -1, 0).mul(camera.view.invMatrix)
    chunkLoadingDirection.set(vec4.x, vec4.y, vec4.z) // new Vector3d(0, 0, -1).rotateX(-player.rotation.x).rotateY(-player.rotation.y))
  }

  private def updateLoadedColumns(): Unit = {
    val rDistSq = math.pow(renderDistance, 2)
    val origin = {
      val temp = chunkLoadingOrigin.toBlockCoords
      new Vector2d(temp.x / 16, temp.z / 16)
    }
    def inSight(col: ColumnRelWorld): Boolean = {
      col.distSq(origin) <= rDistSq
    }

    val here = ColumnRelWorld(math.floor(origin.x).toInt, math.floor(origin.y).toInt, size)
    ensureColumnExists(here)

    val columnsToAdd = MutableSet.empty[ColumnRelWorld]
    val columnsToRemove = MutableSet.empty[ColumnRelWorld]

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

  private def loadChunks(): Unit = {
    def topBottomChangeWhenExists(chY: Int, top: Int, bottom: Int): Option[(Int, Int)] = {
      if (chY == top + 1) Some((chY, bottom))
      else if (chY == bottom - 1) Some((top, chY))
      else None
    }

    def getTopBottomChange(col: ChunkColumn, chY: Int): Option[(Int, Int)] = {
      col.topAndBottomChunks match {
        case None => Some((chY, chY))
        case Some((top, bottom)) =>
          topBottomChangeWhenExists(chY, top, bottom)
      }
    }

    def manageTopBottomFor(chunkToLoad: ChunkRelWorld): Unit = {
      getColumn(chunkToLoad.getColumnRelWorld).foreach { col =>
        val ch = chunkToLoad.getChunkRelColumn
        val topBottomChange = getTopBottomChange(col, ch.Y)

        if (topBottomChange.isDefined) {
          col.topAndBottomChunks = topBottomChange
          val newChunk = new Chunk(chunkToLoad, this)
          col.chunks(ch.value) = newChunk
          chunkAddedOrRemovedListeners.foreach(_.onChunkAdded(newChunk))
        }
      }
    }

    for (_ <- 1 to World.chunksLoadedPerTick) {
      if (chunksToLoad.nonEmpty) {
        val chunkToLoad = chunksToLoad.dequeue()._2
        manageTopBottomFor(chunkToLoad)
      }
    }
  }

  private val chunkRenderUpdateQueue: mutable.PriorityQueue[(Double, ChunkRelWorld)] = mutable.PriorityQueue.empty(Ordering.by(-_._1))

  private val blocksToUpdate = mutable.Queue.empty[BlockRelWorld]

  val player = new Player(this)
  player.fromNBT(worldSettings.playerNBT)

  private[storage] val chunkAddedOrRemovedListeners: ArrayBuffer[ChunkAddedOrRemovedListener] = ArrayBuffer.empty
  def addChunkAddedOrRemovedListener(listener: ChunkAddedOrRemovedListener): Unit = chunkAddedOrRemovedListeners += listener


  def onBlockNeedsUpdate(coords: BlockRelWorld): Unit = blocksToUpdate.enqueue(coords)

  def onChunkNeedsRenderUpdate(coords: ChunkRelWorld): Unit = {
    if (!chunkRenderUpdateQueue.exists(_._2 == coords)) chunkRenderUpdateQueue.enqueue(makeChunkToLoadTuple(coords))
  }

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
    setChunkLoadingCenterAndDirection(camera)

    loadColumnsTimer.tick()

    loadChunks()

    performChunkRenderUpdates()

    columns.values.foreach(_.tick())
  }

  private def performChunkRenderUpdates(): Unit = {
//    println("Chunks: " + columns.map(_._2.chunks.values.size).sum)
//    if (chunkRenderUpdateQueue.nonEmpty) println(chunkRenderUpdateQueue.size)
    for (_ <- 1 to World.chunkRenderUpdatesPerTick) {
      if (chunkRenderUpdateQueue.nonEmpty) {
        var chunk: Option[Chunk] = None
        do {
          chunk = getChunk(chunkRenderUpdateQueue.dequeue()._2)
        } while (chunk.isEmpty && chunkRenderUpdateQueue.nonEmpty)

        chunk.foreach(_.doRenderUpdate())
      }
    }
  }

  private val loadColumnsTimer: TickableTimer = TickableTimer(World.ticksBetweenColumnLoading) {
    performBlockUpdates()

    filterChunkRenderUpdateQueue()

    updateLoadedColumns()
    columns.values.foreach(_.updateLoadedChunks())
  }

  private def filterChunkRenderUpdateQueue(): Unit = {
    val rDistSq = (renderDistance * 16) * (renderDistance * 16)

    //        chunksToLoad.enqueue(          chunksToLoad.dequeueAll.map(c => makeChunkToLoadTuple(c._2)).filter(c => c._1 <= rDistSq): _*)
    chunksToLoad.clear()
    chunkRenderUpdateQueue.enqueue(chunkRenderUpdateQueue.dequeueAll.map(c => makeChunkToLoadTuple(c._2)).filter(c => c._1 <= rDistSq): _*)
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

  def unload(): Unit = {
    val worldTag = NBTUtil.makeCompoundTag("world", Seq(
      NBTUtil.makeCompoundTag("general", Seq(
        new ByteTag("worldSize", size.worldSize.toByte),
        new StringTag("name", worldName)
      )),
      worldGenerator.toNBT,
      player.toNBT
    ))

    worldSettings.saveState(worldTag)

    chunksToLoad.clear
    loadColumnsTimer.active = false
    columns.values.foreach(_.unload())
    columns.clear
  }
}
