package hexacraft.world.loader

import hexacraft.util.TickableTimer
import hexacraft.world.CylinderSize
import hexacraft.world.coord.CoordUtils
import hexacraft.world.coord.fp.BlockCoords
import hexacraft.world.coord.integer.{BlockRelWorld, ChunkRelWorld}

import scala.collection.mutable

object ChunkLoadingPrioritizer {
  def distSq(p: PosAndDir, c: ChunkRelWorld)(using CylinderSize): Double =
    p.pos.distanceSq(BlockCoords(BlockRelWorld(8, 8, 8, c)).toCylCoords)
}

class ChunkLoadingPrioritizer(origin: PosAndDir, maxDist: Double)(using CylinderSize) {
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

  def tick(): Unit = if reorderingTimer.tick() then reorderPQs()

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
