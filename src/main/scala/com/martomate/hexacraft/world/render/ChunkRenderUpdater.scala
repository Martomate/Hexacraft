package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.util.{CylinderSize, TickableTimer, UniquePQ}
import com.martomate.hexacraft.world.camera.Camera
import com.martomate.hexacraft.world.chunk.ChunkEventListener
import com.martomate.hexacraft.world.coord.fp.BlockCoords
import com.martomate.hexacraft.world.coord.integer.{BlockRelWorld, ChunkRelWorld}
import com.martomate.hexacraft.world.loader.PosAndDir

object ChunkRenderUpdater {
  val chunkRenderUpdatesPerTick = 4
  val ticksBetweenColumnLoading = 5
}

class ChunkRenderUpdater(updateChunkIfPresent: ChunkRelWorld => Boolean, renderDistance: => Double)(implicit
    worldSize: CylinderSize
) extends ChunkEventListener {
  private val origin = new PosAndDir

  private val chunkRenderUpdateQueue: UniquePQ[ChunkRelWorld] =
    new UniquePQ(makeChunkToLoadPriority, Ordering.by(-_))

  def update(camera: Camera): Unit = {
    origin.setPosAndDirFrom(camera.view)

    if (reprioritizeTimer.tick()) {
      val rDistSq = (renderDistance * 16) * (renderDistance * 16)

      chunkRenderUpdateQueue.reprioritizeAndFilter(_._1 <= rDistSq)
    }

    val numUpdatesToPerform =
      if (chunkRenderUpdateQueue.size > 10) ChunkRenderUpdater.chunkRenderUpdatesPerTick else 1
    for (_ <- 1 to numUpdatesToPerform) {
      if (!chunkRenderUpdateQueue.isEmpty) {
        while (!updateChunkIfPresent(chunkRenderUpdateQueue.dequeue()) && !chunkRenderUpdateQueue.isEmpty) {}
      }
    }
  }

  private val reprioritizeTimer: TickableTimer = TickableTimer(
    ChunkRenderUpdater.ticksBetweenColumnLoading
  )

  private def makeChunkToLoadPriority(coords: ChunkRelWorld): Double = {
    def distTo(x: Int, y: Int, z: Int): Double = {
      val cyl = BlockCoords(BlockRelWorld(x, y, z, coords)).toCylCoords
      val cDir = cyl.toNormalCoords(origin.pos).toVector3d.normalize()
      val dot = origin.dir.dot(cDir)
      origin.pos.distanceSq(cyl) * (1.25 - math.pow((dot + 1) / 2, 4)) / 1.25
    }
    var dist = distTo(8, 8, 8)
    if (dist < 16) { // if it's close, refine estimate
      for (n <- 0 until 8) {
        val i = n & 1
        val j = n >> 1 & 1
        val k = n >> 2 & 1
        dist = math.min(dist, distTo(15 * i, 15 * j, 15 * k))
      }
    }
    dist
  }

  override def onBlockNeedsUpdate(coords: BlockRelWorld): Unit = () // TODO: Interface Segregation

  override def onChunkNeedsRenderUpdate(coords: ChunkRelWorld): Unit =
    chunkRenderUpdateQueue.enqueue(coords)

  override def onChunksNeighborNeedsRenderUpdate(coords: ChunkRelWorld, side: Int): Unit = ()
}
