package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.Camera
import com.martomate.hexacraft.util.{TickableTimer, UniquePQ}
import com.martomate.hexacraft.world.coord.{BlockCoords, BlockRelWorld, ChunkRelWorld, CylCoords}
import com.martomate.hexacraft.world.storage.{ChunkEventListener, World}
import org.joml.{Vector3d, Vector4d}

class ChunkRenderUpdater(chunkRendererProvider: ChunkRelWorld => Option[ChunkRenderer], renderDistance: => Double) extends ChunkEventListener {
  private var chunkLoadingOrigin: CylCoords = _
  private val chunkLoadingDirection: Vector3d = new Vector3d()
  private def setChunkLoadingCenterAndDirection(camera: Camera): Vector3d = {
    chunkLoadingOrigin = CylCoords(camera.position.x, camera.position.y, camera.position.z, camera.worldSize)
    val vec4 = new Vector4d(0, 0, -1, 0).mul(camera.view.invMatrix)
    chunkLoadingDirection.set(vec4.x, vec4.y, vec4.z) // new Vector3d(0, 0, -1).rotateX(-player.rotation.x).rotateY(-player.rotation.y))
  }

  private val chunkRenderUpdateQueue: UniquePQ[ChunkRelWorld] = new UniquePQ(makeChunkToLoadPriority, Ordering.by(-_))

  def update(camera: Camera): Unit = {
    setChunkLoadingCenterAndDirection(camera)

    reprioritizeTimer.tick()

    for (_ <- 1 to World.chunkRenderUpdatesPerTick) {
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

  private val reprioritizeTimer: TickableTimer = TickableTimer(World.ticksBetweenColumnLoading) {
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
      if (chunkLoadingOrigin == null) chunkLoadingOrigin = CylCoords(0, 0, 0, coords.cylSize)
      val cyl = BlockCoords(coords.withBlockCoords(t._1, t._2, t._3), coords.cylSize).toCylCoords
      val cDir = cyl.toNormalCoords(chunkLoadingOrigin).toVector3d.normalize()
      val dot = chunkLoadingDirection.dot(cDir)
      chunkLoadingOrigin.distanceSq(cyl) * (1.25 - math.pow((dot + 1) / 2, 4)) / 1.25
    }).min

    dist
  }

  override def onBlockNeedsUpdate(coords: BlockRelWorld): Unit = ()// TODO: Interface Segregation

  override def onChunkNeedsRenderUpdate(coords: ChunkRelWorld): Unit = chunkRenderUpdateQueue.enqueue(coords)
}
