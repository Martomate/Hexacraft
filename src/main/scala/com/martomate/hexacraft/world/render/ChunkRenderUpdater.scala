package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.util.{CylinderSize, TickableTimer, UniquePQ}
import com.martomate.hexacraft.world.camera.Camera
import com.martomate.hexacraft.world.chunk.Chunk
import com.martomate.hexacraft.world.coord.fp.BlockCoords
import com.martomate.hexacraft.world.coord.integer.{BlockRelWorld, ChunkRelWorld}
import com.martomate.hexacraft.world.loader.PosAndDir

object ChunkRenderUpdater:
  private val chunkRenderUpdatesPerTick = 4
  private val ticksBetweenColumnLoading = 5

class ChunkRenderUpdater(updateChunkIfPresent: ChunkRelWorld => Boolean)(using CylinderSize):
  private val origin = new PosAndDir

  private val chunkRenderUpdateQueue: UniquePQ[ChunkRelWorld] =
    new UniquePQ(makeChunkToLoadPriority, Ordering.by(-_))

  def update(camera: Camera, renderDistance: Double): Unit =
    origin.setPosAndDirFrom(camera.view)

    if reprioritizeTimer.tick() then
      val rDistSq = (renderDistance * 16) * (renderDistance * 16)
      chunkRenderUpdateQueue.reprioritizeAndFilter(_._1 <= rDistSq)

    val numUpdatesToPerform =
      if chunkRenderUpdateQueue.size > 10
      then ChunkRenderUpdater.chunkRenderUpdatesPerTick
      else 1

    for _ <- 1 to numUpdatesToPerform do
      if !chunkRenderUpdateQueue.isEmpty then
        while (!updateChunkIfPresent(chunkRenderUpdateQueue.dequeue()) && !chunkRenderUpdateQueue.isEmpty) {}

  private val reprioritizeTimer: TickableTimer = TickableTimer(
    ChunkRenderUpdater.ticksBetweenColumnLoading
  )

  private def makeChunkToLoadPriority(coords: ChunkRelWorld): Double =
    def distTo(x: Int, y: Int, z: Int): Double =
      val cyl = BlockCoords(BlockRelWorld(x, y, z, coords)).toCylCoords
      val cDir = cyl.toNormalCoords(origin.pos).toVector3d.normalize()
      val dot = origin.dir.dot(cDir)
      origin.pos.distanceSq(cyl) * (1.25 - math.pow((dot + 1) / 2, 4)) / 1.25

    var dist = distTo(8, 8, 8)
    if dist < 16 then // if it's close, refine estimate
      for n <- 0 until 8 do
        val i = n & 1
        val j = n >> 1 & 1
        val k = n >> 2 & 1
        dist = math.min(dist, distTo(15 * i, 15 * j, 15 * k))

    dist

  def onChunkEvent(event: Chunk.Event): Unit =
    event match
      case Chunk.Event.ChunkNeedsRenderUpdate(coords) => chunkRenderUpdateQueue.enqueue(coords)
      case _                                          =>
