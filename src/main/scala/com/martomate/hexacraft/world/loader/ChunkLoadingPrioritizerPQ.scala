package com.martomate.hexacraft.world.loader

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.coord.CoordUtils
import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld

import scala.collection.mutable

class ChunkLoadingPrioritizerPQ(
    origin: PosAndDir,
    distSqFunc: (PosAndDir, ChunkRelWorld) => Double,
    maxDist: Double
)(implicit cylSize: CylinderSize)
    extends ChunkLoadingPrioritizer
    with ChunkLoadingEdgeListener {

  private val edge = new ChunkLoadingEdge
  edge.addListener(this)

  private val addableChunks: mutable.PriorityQueue[ChunkRelWorld] =
    mutable.PriorityQueue.empty(Ordering.by(c => -distSqFunc(origin, c)))
  private val removableChunks: mutable.PriorityQueue[ChunkRelWorld] =
    mutable.PriorityQueue.empty(Ordering.by(c => distSqFunc(origin, c)))

  private val maxDistSqInBlocks: Double = (maxDist * 16) * (maxDist * 16)

  private val reorderPQsTimerLength = 60
  private var reorderPQsTimer = reorderPQsTimerLength

  override def +=(chunk: ChunkRelWorld): Unit = {
    edge.loadChunk(chunk)
  }

  override def -=(chunk: ChunkRelWorld): Unit = {
    edge.unloadChunk(chunk)
  }

  override def tick(): Unit = {
    reorderPQsTimer -= 1
    if (reorderPQsTimer == 0) {
      reorderPQsTimer = reorderPQsTimerLength
      reorderPQs()
    }
  }

  def reorderPQs(): Unit = {
    val addSeq = addableChunks.toSeq
    addableChunks.clear()
    addableChunks.enqueue(addSeq: _*)
    val remSeq = removableChunks.toSeq
    removableChunks.clear()
    removableChunks.enqueue(remSeq: _*)
  }

  override def nextAddableChunk: Option[ChunkRelWorld] = {
    while (addableChunks.nonEmpty && !edge.canLoad(addableChunks.head)) addableChunks.dequeue()

    if (addableChunks.isEmpty) {
      val coords = CoordUtils.approximateChunkCoords(origin.pos)
      if (!edge.isLoaded(coords)) Some(coords) else None
    } else {
      Some(addableChunks.head).filter(distSqFunc(origin, _) <= maxDistSqInBlocks)
    }
  }

  override def nextRemovableChunk: Option[ChunkRelWorld] = {
    while (removableChunks.nonEmpty && !edge.onEdge(removableChunks.head)) removableChunks.dequeue()

    if (removableChunks.nonEmpty)
      Some(removableChunks.head).filter(distSqFunc(origin, _) > maxDistSqInBlocks)
    else
      None
  }

  override def unload(): Unit = ()

  override def onChunkOnEdge(chunk: ChunkRelWorld, onEdge: Boolean): Unit = {
    if (onEdge)
      removableChunks += chunk
  }

  override def onChunkLoadable(chunk: ChunkRelWorld, loadable: Boolean): Unit = {
    if (loadable)
      addableChunks += chunk
  }
}
