package hexacraft.world

import hexacraft.util.{EventDispatcher, TickableTimer, Tracker}
import hexacraft.world.chunk.Chunk
import hexacraft.world.coord.{BlockCoords, BlockRelWorld, ChunkRelWorld, CoordUtils, CylCoords}

import java.util.concurrent.TimeUnit
import scala.collection.mutable
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

class ChunkLoader(
    chunkFactory: ChunkRelWorld => Chunk,
    chunkUnloader: ChunkRelWorld => Unit,
    maxDist: Double
)(using CylinderSize) {
  import scala.concurrent.ExecutionContext.Implicits.global

  private val LoadsPerTick = 1
  private val UnloadsPerTick = 2
  private val MaxChunksToLoad = 4
  private val MaxChunksToUnload = 4

  private val prioritizer = new ChunkLoadingPrioritizer(maxDist)

  private def distSqFunc(p: PosAndDir, c: ChunkRelWorld): Double =
    p.pos.distanceSq(BlockCoords(BlockRelWorld(8, 8, 8, c)).toCylCoords)

  private val chunksLoading: mutable.Map[ChunkRelWorld, Future[Chunk]] = mutable.Map.empty
  private val chunksUnloading: mutable.Map[ChunkRelWorld, Future[ChunkRelWorld]] = mutable.Map.empty

  def tick(origin: PosAndDir, shouldLoadSlowly: Boolean): (Seq[Chunk], Seq[ChunkRelWorld]) =
    prioritizer.tick(origin)

    val (maxLoad, maxUnload) = if shouldLoadSlowly then (1, 1) else (MaxChunksToLoad, MaxChunksToUnload)

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

    val chunksToAdd = chunksLoading.values.flatMap(_.value).flatMap(_.toOption).toSeq
    val chunksToRemove = chunksUnloading.values.flatMap(_.value).flatMap(_.toOption).toSeq

    (chunksToAdd, chunksToRemove)

  def onWorldEvent(event: World.Event): Unit =
    event match
      case World.Event.ChunkAdded(chunk)    => chunksLoading -= chunk.coords
      case World.Event.ChunkRemoved(coords) => chunksUnloading -= coords
      case _                                =>

  def unload(): Unit =
    for f <- chunksLoading.values do Await.result(f, Duration(10, TimeUnit.SECONDS))
    for f <- chunksUnloading.values do Await.result(f, Duration(10, TimeUnit.SECONDS))
}

object ChunkLoadingPrioritizer {
  def distSq(p: PosAndDir, c: ChunkRelWorld)(using CylinderSize): Double =
    p.pos.distanceSq(BlockCoords(BlockRelWorld(8, 8, 8, c)).toCylCoords)
}

class ChunkLoadingPrioritizer(maxDist: Double)(using CylinderSize) {
  private var origin: PosAndDir = PosAndDir(CylCoords(0, 0, 0))
  private val edge = new ChunkLoadingEdge
  edge.trackEvents(this.onChunkEdgeEvent _)

  private val furthestFirst: Ordering[ChunkRelWorld] = Ordering.by(c => distSq(origin, c))
  private val closestFirst: Ordering[ChunkRelWorld] = Ordering.by(c => -distSq(origin, c))

  private val addableChunks: mutable.PriorityQueue[ChunkRelWorld] = mutable.PriorityQueue.empty(closestFirst)
  private val removableChunks: mutable.PriorityQueue[ChunkRelWorld] = mutable.PriorityQueue.empty(furthestFirst)

  private val maxDistSqInBlocks: Double = (maxDist * 16) * (maxDist * 16)

  private val reorderingTimer = TickableTimer(60)

  private def distSq(p: PosAndDir, c: ChunkRelWorld): Double = ChunkLoadingPrioritizer.distSq(p, c)

  def +=(chunk: ChunkRelWorld): Unit = edge.loadChunk(chunk)

  def -=(chunk: ChunkRelWorld): Unit = edge.unloadChunk(chunk)

  def tick(origin: PosAndDir): Unit =
    this.origin = origin
    if reorderingTimer.tick() then reorderPQs()

  def reorderPQs(): Unit =
    val addSeq = addableChunks.toSeq
    addableChunks.clear()
    addableChunks.enqueue(addSeq*)
    val remSeq = removableChunks.toSeq
    removableChunks.clear()
    removableChunks.enqueue(remSeq*)

  def nextAddableChunk: Option[ChunkRelWorld] =
    while addableChunks.nonEmpty && !edge.canLoad(addableChunks.head)
    do addableChunks.dequeue()

    if addableChunks.nonEmpty
    then Some(addableChunks.head).filter(coords => distSq(origin, coords) <= maxDistSqInBlocks)
    else Some(CoordUtils.approximateChunkCoords(origin.pos)).filter(coords => !edge.isLoaded(coords))

  def nextRemovableChunk: Option[ChunkRelWorld] =
    while removableChunks.nonEmpty && !edge.onEdge(removableChunks.head)
    do removableChunks.dequeue()

    if removableChunks.nonEmpty
    then Some(removableChunks.head).filter(coords => distSq(origin, coords) > maxDistSqInBlocks)
    else None

  def popChunkToLoad: Option[ChunkRelWorld] =
    nextAddableChunk.map: coords =>
      this += coords
      coords

  def popChunkToRemove: Option[ChunkRelWorld] =
    nextRemovableChunk.map: coords =>
      this -= coords
      coords

  private def onChunkEdgeEvent(event: ChunkLoadingEdge.Event): Unit =
    import ChunkLoadingEdge.Event.*
    event match
      case ChunkOnEdge(chunk, onEdge)     => if onEdge then removableChunks += chunk
      case ChunkLoadable(chunk, loadable) => if loadable then addableChunks += chunk
}

object ChunkLoadingEdge {
  enum Event:
    case ChunkOnEdge(chunk: ChunkRelWorld, onEdge: Boolean)
    case ChunkLoadable(chunk: ChunkRelWorld, loadable: Boolean)
}

class ChunkLoadingEdge(using CylinderSize) {
  private val chunksLoaded: mutable.Set[ChunkRelWorld] = mutable.HashSet.empty
  private val chunksEdge: mutable.Set[ChunkRelWorld] = mutable.HashSet.empty
  private val chunksLoadable: mutable.Set[ChunkRelWorld] = mutable.HashSet.empty

  private val dispatcher = new EventDispatcher[ChunkLoadingEdge.Event]
  def trackEvents(tracker: Tracker[ChunkLoadingEdge.Event]): Unit = dispatcher.track(tracker)

  def isLoaded(chunk: ChunkRelWorld): Boolean = chunksLoaded.contains(chunk)

  def onEdge(chunk: ChunkRelWorld): Boolean = chunksEdge.contains(chunk)

  def canLoad(chunk: ChunkRelWorld): Boolean = chunksLoadable.contains(chunk)

  def loadChunk(chunk: ChunkRelWorld): Unit =
    setLoaded(chunk, true)
    setOnEdge(chunk, true)
    setLoadable(chunk, false)

    for n <- chunk.neighbors
    do
      if n.neighbors.forall(isLoaded) then setOnEdge(n, false)
      if !isLoaded(n) then setLoadable(n, true)

  def unloadChunk(chunk: ChunkRelWorld): Unit =
    setLoaded(chunk, false)
    setOnEdge(chunk, false)
    setLoadable(chunk, chunk.neighbors.exists(isLoaded))

    for n <- chunk.neighbors
    do
      if !n.neighbors.exists(isLoaded) then setLoadable(n, false)
      if isLoaded(n) then setOnEdge(n, onEdge = true)

  private def setLoaded(chunk: ChunkRelWorld, loaded: Boolean): Unit =
    if chunksLoaded.contains(chunk) != loaded then chunksLoaded(chunk) = loaded

  private def setOnEdge(chunk: ChunkRelWorld, onEdge: Boolean): Unit =
    if chunksEdge.contains(chunk) != onEdge then
      chunksEdge(chunk) = onEdge
      dispatcher.notify(ChunkLoadingEdge.Event.ChunkOnEdge(chunk, onEdge))

  private def setLoadable(chunk: ChunkRelWorld, loadable: Boolean): Unit =
    if chunksLoadable.contains(chunk) != loadable then
      chunksLoadable(chunk) = loadable
      dispatcher.notify(ChunkLoadingEdge.Event.ChunkLoadable(chunk, loadable))
}
