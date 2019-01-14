package com.martomate.hexacraft.world.loader

import com.martomate.hexacraft.util.{CylinderSize, TickableTimer, UniquePQ}
import com.martomate.hexacraft.world.chunk.IChunk
import com.martomate.hexacraft.world.column.ChunkColumn
import com.martomate.hexacraft.world.coord.fp.BlockCoords
import com.martomate.hexacraft.world.coord.integer._
import org.joml.Vector2d

import scala.collection.mutable

class ChunkLoaderWithOrigin(worldSize: CylinderSize,
                            loadingDistance: Double,
                            @deprecated columns: mutable.Map[Long, ChunkColumn],
                            @deprecated columnFactory: ColumnRelWorld => ChunkColumn,
                            chunkFactory: ChunkRelWorld => IChunk,
                            origin: PosAndDir) extends ChunkLoader {
  import worldSize.impl

  def tick(): Unit = {
    loadChunks()

    loadColumnsTimer.tick()
  }

  private val chunksToLoad: UniquePQ[ChunkRelWorld] = new UniquePQ(makeChunkToLoadPriority, Ordering.by(-_))
  private val chunksReadyToAdd: mutable.Set[IChunk] = mutable.Set.empty
  private val chunksReadyToRemove: mutable.Set[ChunkRelWorld] = mutable.Set.empty

  @deprecated
  private val columnsAtEdge: mutable.Set[ColumnRelWorld] = mutable.Set.empty[ColumnRelWorld]

  @deprecated
  private val topAndBottomChunks: mutable.Map[ColumnRelWorld, Option[(Int, Int)]] = mutable.HashMap.empty

  private def makeChunkToLoadPriority(coords: ChunkRelWorld): Double = {
    def distTo(x: Int, y: Int, z: Int): Double = {
      val cyl = BlockCoords(BlockRelWorld(x, y, z, coords)).toCylCoords
      val cDir = cyl.toNormalCoords(origin.pos).toVector3d.normalize()
      val dot = this.origin.dir.dot(cDir)
      origin.pos.distanceSq(cyl) * (1.25 - math.pow((dot + 1) / 2, 4)) / 1.25
    }
    var dist = distTo(8, 8, 8)
    if (dist < 16) {// if it's close, refine estimate
      for (n <- 0 until 8) {
        val i = n & 1
        val j = n >> 1 & 1
        val k = n >> 2 & 1
        dist = math.min(dist, distTo(15 * i, 15 * j, 15 * k))
      }
    }
    dist
  }

  private def loadChunks(): Unit = {
    def topBottomChangeWhenExists(chY: Int, top: Int, bottom: Int): Option[(Int, Int)] = {
      if (chY == top + 1) Some((chY, bottom))
      else if (chY == bottom - 1) Some((top, chY))
      else None
    }

    def getTopBottomChange(col: ColumnRelWorld, chY: Int): Option[(Int, Int)] = {
      topAndBottomChunks.getOrElse(col, None) match {
        case None => Some((chY, chY))
        case Some((top, bottom)) =>
          topBottomChangeWhenExists(chY, top, bottom)
      }
    }

    def manageTopBottomFor(chunkToLoad: ChunkRelWorld): Boolean = {// returns true if it loaded a non-empty chunk
      val col = chunkToLoad.getColumnRelWorld
      val ch = chunkToLoad.getChunkRelColumn
      val topBottomChange = getTopBottomChange(col, ch.Y)

      if (topBottomChange.isDefined) {
        topAndBottomChunks(col) = topBottomChange
        val newChunk = chunkFactory(chunkToLoad)
        chunksReadyToAdd += newChunk
//          !newChunk.isEmpty // not a good idea, since even an empty chunk might have to load to see that it is empty
        true
      } else false
    }

    for (_ <- 1 to ChunkLoader.chunksLoadedPerTick) {
      while (!chunksToLoad.isEmpty && !manageTopBottomFor(chunksToLoad.dequeue())) {}
    }
  }

  private[loader] def updateLoadedChunks(column: ChunkColumn): Unit = {
    val origin = this.origin.pos.toBlockCoords.toVector3d.div(16)
    val xzDist = math.sqrt(column.coords.distSq(new Vector2d(origin.x, origin.z)))

    def inSight(chunk: ChunkRelColumn): Boolean = {
      val dy = chunk.Y - origin.y
      math.max(math.abs(dy), xzDist) <= loadingDistance
    }

    def newTopOrBottom(now: Int, dir: Int): Int = {
      val bottomChunk = ChunkRelColumn(now & 0xfff)
      if (inSight(bottomChunk)) {
        val below = ChunkRelColumn((now + dir) & 0xfff)
        if (inSight(below)) addChunkToLoadingQueue(ChunkRelWorld(below, column.coords))
        now
      } else {
        chunksReadyToRemove += ChunkRelWorld(bottomChunk, column.coords)
        now - dir
      }
    }

    topAndBottomChunks.getOrElse(column.coords, None) match {
      case Some((top, bottom)) =>
        val newBottom = newTopOrBottom(bottom, -1)
        val newTop = if (newBottom > top) top else newTopOrBottom(top, 1)

        if (newTop != top || newBottom != bottom) topAndBottomChunks(column.coords) = if (newTop >= newBottom) Some((newTop, newBottom)) else None
      case None =>
        val ground = ChunkRelColumn((column.generatedHeightMap(8)(8) >> 4) & 0xfff)
        val first = if (inSight(ground)) ground else ChunkRelColumn(math.round(origin.y).toInt & 0xfff)

        if (inSight(first)) addChunkToLoadingQueue(ChunkRelWorld(first, column.coords))
    }
  }

  private def addChunkToLoadingQueue(coords: ChunkRelWorld): Unit = {
    chunksToLoad.enqueue(coords)
  }

  private[loader] def filterChunkLoadingQueue(): Unit = {
    val rDistSq = (loadingDistance * 16) * (loadingDistance * 16)

    chunksToLoad.reprioritizeAndFilter(_._1 <= rDistSq)
  }

  private val loadColumnsTimer: TickableTimer = TickableTimer(ChunkLoader.ticksBetweenColumnLoading) {
    updateLoadedColumns()

    filterChunkLoadingQueue()
    columns.values.foreach(updateLoadedChunks)
  }

  private def updateLoadedColumns(): Unit = {
    val rDistSq = math.pow(loadingDistance, 2)
    val origin = {
      val temp = this.origin.pos.toBlockCoords
      new Vector2d(temp.x / 16, temp.z / 16)
    }
    def inSight(col: ColumnRelWorld): Boolean = {
      col.distSq(origin) <= rDistSq
    }

    val here = ColumnRelWorld(math.floor(origin.x).toInt, math.floor(origin.y).toInt)
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
            columns(col2.value) = columnFactory(col2)// TODO: Make sure columnsAtEdge is updated when columns is updated from World
          }
        } else {
          surrounded = false
        }
      }
      surrounded
    }

    def shouldRemoveColAfterManagingEdgeAt(col: ColumnRelWorld): Boolean = {
      if (!inSight(col)) {
//        columns.remove(col.value).get.unload()
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

  private def ensureColumnExists(here: ColumnRelWorld): Unit = {
    if (!columns.contains(here.value)) {
      columns(here.value) = columnFactory(here)
      columnsAtEdge += here
    }
  }

  def unload(): Unit = {
    chunksToLoad.clear()
    chunksReadyToAdd.foreach(_.unload())
    chunksReadyToAdd.clear()
  }

  @deprecated
  override def onColumnAdded(column: ChunkColumn): Unit = columnsAtEdge += column.coords

  @deprecated
  override def onColumnRemoved(column: ChunkColumn): Unit = columnsAtEdge -= column.coords

  override def onChunkAdded(chunk: IChunk): Unit = {
    val coords = chunk.coords
    chunksReadyToAdd -= chunk

/*    if (!chunksLoaded(coords)) {
      chunksLoaded += coords

      if (coords.neighbors.exists(neigh => !chunksLoaded(neigh))) {
        chunksOnInnerEdge += coords
      }
    }*/
  }

  override def onChunkRemoved(chunk: IChunk): Unit = {
    val coords = chunk.coords
    chunksReadyToRemove -= coords

/*    if (chunksLoaded(coords)) {
      chunksLoaded -= coords

      for (neigh <- coords.neighbors) {
        chunksOnInnerEdge += neigh
      }
    }*/
  }

  override def chunksToAdd(): Iterable[IChunk] = chunksReadyToAdd

  override def chunksToRemove(): Iterable[ChunkRelWorld] = chunksReadyToRemove
}
