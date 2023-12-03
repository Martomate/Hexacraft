package hexacraft.world.loader

import hexacraft.world.{CylinderSize, World}
import hexacraft.world.chunk.Chunk
import hexacraft.world.coord.fp.BlockCoords
import hexacraft.world.coord.integer.{BlockRelWorld, ChunkRelWorld}

import scala.collection.mutable
import scala.concurrent.Future

class ChunkLoader(
    origin: PosAndDir,
    chunkFactory: ChunkRelWorld => Chunk,
    chunkUnloader: ChunkRelWorld => Unit,
    maxDist: Double,
    shouldLoadSlowly: () => Boolean
)(using CylinderSize) {
  import scala.concurrent.ExecutionContext.Implicits.global

  private val LoadsPerTick = 1
  private val UnloadsPerTick = 2
  private val MaxChunksToLoad = 4
  private val MaxChunksToUnload = 4

  private val prioritizer = new ChunkLoadingPrioritizer(origin, maxDist)

  private def distSqFunc(p: PosAndDir, c: ChunkRelWorld): Double =
    p.pos.distanceSq(BlockCoords(BlockRelWorld(8, 8, 8, c)).toCylCoords)

  private val chunksLoading: mutable.Map[ChunkRelWorld, Future[Chunk]] = mutable.Map.empty
  private val chunksUnloading: mutable.Map[ChunkRelWorld, Future[ChunkRelWorld]] = mutable.Map.empty

  def tick(): Unit =
    prioritizer.tick()
    val (maxLoad, maxUnload) = if shouldLoadSlowly() then (1, 1) else (MaxChunksToLoad, MaxChunksToUnload)

    for _ <- 1 to LoadsPerTick do
      if chunksLoading.size < maxLoad then
        prioritizer.popChunkToLoad.foreach: coords =>
          chunksLoading(coords) = Future(chunkFactory(coords))

    for _ <- 1 to UnloadsPerTick do
      if chunksUnloading.size < maxUnload then
        prioritizer.popChunkToRemove.foreach: coords =>
          chunksUnloading(coords) = Future:
            chunkUnloader(coords)
            coords

  def chunksToAdd(): Iterable[Chunk] =
    chunksLoading.values.flatMap(_.value).flatMap(_.toOption)

  def chunksToRemove(): Iterable[ChunkRelWorld] =
    chunksUnloading.values.flatMap(_.value).flatMap(_.toOption)

  def onWorldEvent(event: World.Event): Unit =
    event match
      case World.Event.ChunkAdded(chunk)    => chunksLoading -= chunk.coords
      case World.Event.ChunkRemoved(coords) => chunksUnloading -= coords
}
