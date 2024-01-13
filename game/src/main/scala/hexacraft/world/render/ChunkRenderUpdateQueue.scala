package hexacraft.world.render

import hexacraft.util.UniquePQ
import hexacraft.world.{Camera, CylinderSize, PosAndDir}
import hexacraft.world.coord.{BlockCoords, BlockRelWorld, ChunkRelWorld, CylCoords}

class ChunkRenderUpdateQueue(using CylinderSize):
  private val origin = PosAndDir(CylCoords(0, 0, 0))

  private val queue: UniquePQ[ChunkRelWorld] = new UniquePQ(makeChunkToUpdatePriority, Ordering.by(-_))

  def reorderAndFilter(camera: Camera, renderDistance: Double): Unit =
    origin.setPosAndDirFrom(camera.view)

    val rDistSq = (renderDistance * 16) * (renderDistance * 16)
    queue.reprioritizeAndFilter(_._1 <= rDistSq)

  def length: Int = queue.size

  def pop(): Option[ChunkRelWorld] = if !queue.isEmpty then Some(queue.dequeue()) else None

  def insert(coords: ChunkRelWorld): Unit = queue.enqueue(coords)

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
