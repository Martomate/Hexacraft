package hexacraft.world.loader

import hexacraft.util.{EventDispatcher, Tracker}
import hexacraft.world.CylinderSize
import hexacraft.world.coord.integer.ChunkRelWorld

import scala.collection.mutable

object ChunkLoadingEdge {
  enum Event:
    case ChunkOnEdge(chunk: ChunkRelWorld, onEdge: Boolean)
    case ChunkLoadable(chunk: ChunkRelWorld, loadable: Boolean)
}

class ChunkLoadingEdge(implicit cylSize: CylinderSize) {
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
