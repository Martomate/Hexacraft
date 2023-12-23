package hexacraft.world.render

import hexacraft.util.{TickableTimer, UniquePQ}
import hexacraft.world.{Camera, CylinderSize, PosAndDir}
import hexacraft.world.chunk.Chunk
import hexacraft.world.coord.{BlockCoords, BlockRelWorld, ChunkRelWorld, CylCoords}

object ChunkRenderUpdater:
  private val chunkRenderUpdatesPerTick = 4
  private val ticksBetweenColumnLoading = 5

class ChunkRenderUpdater(using CylinderSize):
  private val origin = PosAndDir(CylCoords(0, 0, 0))

  private val chunkRenderUpdateQueue: UniquePQ[ChunkRelWorld] = new UniquePQ(makeChunkToUpdatePriority, Ordering.by(-_))

  private val reorderingTimer: TickableTimer = TickableTimer(ChunkRenderUpdater.ticksBetweenColumnLoading)

  def update(camera: Camera, renderDistance: Double, updateChunkIfPresent: ChunkRelWorld => Boolean): Unit =
    origin.setPosAndDirFrom(camera.view)

    if reorderingTimer.tick() then
      val rDistSq = (renderDistance * 16) * (renderDistance * 16)
      chunkRenderUpdateQueue.reprioritizeAndFilter(_._1 <= rDistSq)

    val numUpdatesToPerform =
      if chunkRenderUpdateQueue.size > 10
      then ChunkRenderUpdater.chunkRenderUpdatesPerTick
      else 1

    for _ <- 1 to numUpdatesToPerform do
      while !chunkRenderUpdateQueue.isEmpty && !updateChunkIfPresent(chunkRenderUpdateQueue.dequeue()) do ()

  private def makeChunkToUpdatePriority(coords: ChunkRelWorld): Double =
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
      case Chunk.Event.ChunkNeedsRenderUpdate(coords) =>
        chunkRenderUpdateQueue.enqueue(coords)
      case _ =>
