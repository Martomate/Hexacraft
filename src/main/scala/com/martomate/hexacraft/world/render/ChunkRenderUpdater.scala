package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.Camera
import com.martomate.hexacraft.util.{TickableTimer, UniquePQ}
import com.martomate.hexacraft.world.coord.{BlockCoords, BlockRelWorld, ChunkRelWorld}
import com.martomate.hexacraft.world.storage.ChunkEventListener
import com.martomate.hexacraft.world.{CylinderSize, PosAndDir}

object ChunkRenderUpdater {
  val chunkRenderUpdatesPerTick = 1
  val ticksBetweenColumnLoading = 5
}

class ChunkRenderUpdater(chunkRendererProvider: ChunkRelWorld => Option[ChunkRenderer], renderDistance: => Double, worldSize: CylinderSize) extends ChunkEventListener {
  private val origin = new PosAndDir(worldSize)

  private val chunkRenderUpdateQueue: UniquePQ[ChunkRelWorld] = new UniquePQ(makeChunkToLoadPriority, Ordering.by(-_))

  def update(camera: Camera): Unit = {
    origin.setPosAndDirFrom(camera.view)

    reprioritizeTimer.tick()

    for (_ <- 1 to ChunkRenderUpdater.chunkRenderUpdatesPerTick) {
      if (!chunkRenderUpdateQueue.isEmpty) {
        var renderer: Option[ChunkRenderer] = None
        do {
          val coords = chunkRenderUpdateQueue.dequeue()
          renderer = chunkRendererProvider(coords)
        } while (renderer.isEmpty && !chunkRenderUpdateQueue.isEmpty)

        renderer.foreach(_.updateContent())
      }
    }
  }

  private val reprioritizeTimer: TickableTimer = TickableTimer(ChunkRenderUpdater.ticksBetweenColumnLoading) {
    val rDistSq = (renderDistance * 16) * (renderDistance * 16)

    chunkRenderUpdateQueue.reprioritizeAndFilter(_._1 <= rDistSq)
  }

  private def makeChunkToLoadPriority(coords: ChunkRelWorld): Double = {
    val corners = for {
      i <- 0 to 1
      j <- 0 to 1
      k <- 0 to 1
    } yield (15 * i, 15 * j, 15 * k)
    val dist = ((corners :+ (8, 8, 8)) map { t =>
      val cyl = BlockCoords(coords.withBlockCoords(t._1, t._2, t._3), coords.cylSize).toCylCoords
      val cDir = cyl.toNormalCoords(origin.pos).toVector3d.normalize()
      val dot = origin.dir.dot(cDir)
      origin.pos.distanceSq(cyl) * (1.25 - math.pow((dot + 1) / 2, 4)) / 1.25
    }).min

    dist
  }

  override def onBlockNeedsUpdate(coords: BlockRelWorld): Unit = ()// TODO: Interface Segregation

  override def onChunkNeedsRenderUpdate(coords: ChunkRelWorld): Unit = chunkRenderUpdateQueue.enqueue(coords)

  override def onChunksNeighborNeedsRenderUpdate(coords: ChunkRelWorld, side: Int): Unit = ()
}
