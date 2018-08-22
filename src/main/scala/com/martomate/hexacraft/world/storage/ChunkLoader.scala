package com.martomate.hexacraft.world.storage

import com.martomate.hexacraft.Camera
import com.martomate.hexacraft.util.{TickableTimer, UniquePQ}
import com.martomate.hexacraft.world.coord.{BlockCoords, ChunkRelColumn, ChunkRelWorld, CylCoords}
import org.joml.{Vector2d, Vector3d, Vector4d}

class ChunkLoader(world: World) {
  def tick(camera: Camera): Unit = {
    setChunkLoadingCenterAndDirection(camera)
    loadChunks()

    loadColumnsTimer.tick()
  }

  private[storage] var chunkLoadingOrigin: CylCoords = _
  private[storage] val chunkLoadingDirection: Vector3d = new Vector3d()

  private def setChunkLoadingCenterAndDirection(camera: Camera): Vector3d = {
    chunkLoadingOrigin = CylCoords(camera.position.x, camera.position.y, camera.position.z, camera.worldSize)
    val vec4 = new Vector4d(0, 0, -1, 0).mul(camera.view.invMatrix)
    chunkLoadingDirection.set(vec4.x, vec4.y, vec4.z) // new Vector3d(0, 0, -1).rotateX(-player.rotation.x).rotateY(-player.rotation.y))
  }

  val chunksToLoad: UniquePQ[ChunkRelWorld] = new UniquePQ(makeChunkToLoadPriority, Ordering.by(-_))

  private def makeChunkToLoadPriority(coords: ChunkRelWorld): Double = {
    val corners = for {
      i <- 0 to 1
      j <- 0 to 1
      k <- 0 to 1
    } yield (15 * i, 15 * j, 15 * k)
    val dist = ((corners :+ (8, 8, 8)) map { t =>
      if (chunkLoadingOrigin == null) chunkLoadingOrigin = CylCoords(0, 0, 0, coords.cylSize)
      val cyl = BlockCoords(coords.withBlockCoords(t._1, t._2, t._3), coords.cylSize).toCylCoords
      val cDir = cyl.toNormalCoords(chunkLoadingOrigin).toVector3d.normalize()
      val dot = chunkLoadingDirection.dot(cDir)
      chunkLoadingOrigin.distanceSq(cyl) * (1.25 - math.pow((dot + 1) / 2, 4)) / 1.25
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
      world.getColumn(chunkToLoad.getColumnRelWorld).exists { col =>
        val ch = chunkToLoad.getChunkRelColumn
        val topBottomChange = getTopBottomChange(col, ch.Y)

        if (topBottomChange.isDefined) {
          col.topAndBottomChunks = topBottomChange
          val newChunk = new Chunk(chunkToLoad, new ChunkGenerator(chunkToLoad, world), world)
          col.chunks(ch.value) = newChunk
          world.chunkAddedOrRemovedListeners.foreach(_.onChunkAdded(newChunk))
          !newChunk.isEmpty
        } else false
      }
    }

    for (_ <- 1 to World.chunksLoadedPerTick) {
      while (!chunksToLoad.isEmpty && !manageTopBottomFor(chunksToLoad.dequeue())) {}
    }
  }

  private[storage] def updateLoadedChunks(column: ChunkColumn): Unit = {
    val origin = chunkLoadingOrigin.toBlockCoords.toVector3d.div(16)
    val xzDist = math.sqrt(column.coords.distSq(new Vector2d(origin.x, origin.z)))

    def inSight(chunk: ChunkRelColumn): Boolean = {
      val dy = chunk.Y - origin.y
      math.abs(dy) + xzDist * 0.25 <= world.renderDistance
    }

    def newTopOrBottom(now: Int, dir: Int): Int = {
      val bottomChunk = ChunkRelColumn(now & 0xfff, world.size)
      if (inSight(bottomChunk)) {
        val below = ChunkRelColumn((now + dir) & 0xfff, world.size)
        if (inSight(below)) addChunkToLoadingQueue(below.withColumn(column.coords))
        now
      } else {
        column.chunks.remove(bottomChunk.value).foreach{c =>
          world.chunkAddedOrRemovedListeners.foreach(_.onChunkRemoved(c))
          c.unload()
        }
        now - dir
      }
    }

    column.topAndBottomChunks match {
      case Some((top, bottom)) =>
        val newBottom = newTopOrBottom(bottom, -1)
        val newTop = if (newBottom > top) top else newTopOrBottom(top, 1)

        if (newTop != top || newBottom != bottom) column.topAndBottomChunks = if (newTop >= newBottom) Some((newTop, newBottom)) else None
      case None =>
        val ground = ChunkRelColumn((column.generatedHeightMap(8)(8) >> 4) & 0xfff, world.size)
        val first = if (inSight(ground)) ground else ChunkRelColumn(math.round(origin.y).toInt & 0xfff, world.size)

        if (inSight(first)) addChunkToLoadingQueue(first.withColumn(column.coords))
    }
  }

  def addChunkToLoadingQueue(coords: ChunkRelWorld): Unit = {
    chunksToLoad.enqueue(coords)
  }

  private[storage] def filterChunkLoadingQueue(): Unit = {
    val rDistSq = (world.renderDistance * 16) * (world.renderDistance * 16)

    chunksToLoad.reprioritizeAndFilter(_._1 <= rDistSq)
  }

  private val loadColumnsTimer: TickableTimer = TickableTimer(World.ticksBetweenColumnLoading) {
    filterChunkLoadingQueue()
    world.columns.values.foreach(updateLoadedChunks)
  }

  def unload(): Unit = {
    chunksToLoad.clear()
  }
}
