package com.martomate.hexacraft.world.loader

import com.martomate.hexacraft.util.{CylinderSize, TickableTimer, UniquePQ}
import com.martomate.hexacraft.world.chunk.IChunk
import com.martomate.hexacraft.world.coord.fp.BlockCoords
import com.martomate.hexacraft.world.coord.integer._
import com.martomate.hexacraft.world.temp2.ChunkColumn
import org.joml.Vector2d

import scala.collection.mutable

class ChunkLoaderWithOrigin(worldSize: CylinderSize,
                            loadingDistance: Double,
                            @deprecated columns: mutable.Map[Long, ChunkColumn],
                            @deprecated columnFactory: ColumnRelWorld => ChunkColumn,
                            chunkFactory: ChunkRelWorld => IChunk,
                            origin: PosAndDir) extends ChunkLoader {
  def tick(): Unit = {
    loadChunks()

    loadColumnsTimer.tick()
  }

  private val chunksToLoad: UniquePQ[ChunkRelWorld] = new UniquePQ(makeChunkToLoadPriority, Ordering.by(-_))
  private val chunksReadyToAdd: mutable.Set[IChunk] = mutable.Set.empty
  private val chunksReadyToRemove: mutable.Set[ChunkRelWorld] = mutable.Set.empty

  @deprecated
  private val columnsAtEdge: mutable.Set[ColumnRelWorld] = mutable.Set.empty[ColumnRelWorld]

  private def makeChunkToLoadPriority(coords: ChunkRelWorld): Double = {
    val corners = for {
      i <- 0 to 1
      j <- 0 to 1
      k <- 0 to 1
    } yield (15 * i, 15 * j, 15 * k)
    val dist = ((corners :+ (8, 8, 8)) map { t =>
      val cyl = BlockCoords(BlockRelWorld(t._1, t._2, t._3, coords), coords.cylSize).toCylCoords
      val cDir = cyl.toNormalCoords(origin.pos).toVector3d.normalize()
      val dot = this.origin.dir.dot(cDir)
      origin.pos.distanceSq(cyl) * (1.25 - math.pow((dot + 1) / 2, 4)) / 1.25
    }).min

    dist
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

    def manageTopBottomFor(chunkToLoad: ChunkRelWorld): Boolean = {// returns true if it loaded a non-empty chunk
      columns.get(chunkToLoad.getColumnRelWorld.value).exists { col =>
        val ch = chunkToLoad.getChunkRelColumn
        val topBottomChange = getTopBottomChange(col, ch.Y)

        if (topBottomChange.isDefined) {
          col.topAndBottomChunks = topBottomChange
          val newChunk = chunkFactory(chunkToLoad)
          chunksReadyToAdd += newChunk
          !newChunk.isEmpty
        } else false
      }
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
      math.abs(dy) + xzDist * 0.25 <= loadingDistance
    }

    def newTopOrBottom(now: Int, dir: Int): Int = {
      val bottomChunk = ChunkRelColumn(now & 0xfff, worldSize)
      if (inSight(bottomChunk)) {
        val below = ChunkRelColumn((now + dir) & 0xfff, worldSize)
        if (inSight(below)) addChunkToLoadingQueue(ChunkRelWorld(below, column.coords))
        now
      } else {
        chunksReadyToRemove += ChunkRelWorld(bottomChunk, column.coords)
        now - dir
      }
    }

    column.topAndBottomChunks match {
      case Some((top, bottom)) =>
        val newBottom = newTopOrBottom(bottom, -1)
        val newTop = if (newBottom > top) top else newTopOrBottom(top, 1)

        if (newTop != top || newBottom != bottom) column.topAndBottomChunks = if (newTop >= newBottom) Some((newTop, newBottom)) else None
      case None =>
        val ground = ChunkRelColumn((column.generatedHeightMap(8)(8) >> 4) & 0xfff, worldSize)
        val first = if (inSight(ground)) ground else ChunkRelColumn(math.round(origin.y).toInt & 0xfff, worldSize)

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

    val here = ColumnRelWorld(math.floor(origin.x).toInt, math.floor(origin.y).toInt, worldSize)
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
