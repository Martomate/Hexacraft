package hexacraft.world

import hexacraft.util.{Channel, TickableTimer}
import hexacraft.world.coord.*

import scala.collection.mutable

object ChunkLoadingPrioritizer {
  def distSq(p: Pose, c: ChunkRelWorld)(using CylinderSize): Double = {
    p.pos.distanceSq(BlockCoords(BlockRelWorld(8, 8, 8, c)).toCylCoords)
  }
}

class ChunkLoadingPrioritizer(maxDist: Double)(using CylinderSize) {
  private var origin: Pose = Pose(CylCoords(0, 0, 0))
  private val edge = makeChunkLoadingEdge()

  private val furthestFirst: Ordering[ChunkRelWorld] = Ordering.by(c => distSq(origin, c))
  private val closestFirst: Ordering[ChunkRelWorld] = Ordering.by(c => -distSq(origin, c))

  private val addableChunks: mutable.PriorityQueue[ChunkRelWorld] = mutable.PriorityQueue.empty(using closestFirst)
  private val removableChunks: mutable.PriorityQueue[ChunkRelWorld] = mutable.PriorityQueue.empty(using furthestFirst)

  private val maxDistSqInBlocks: Double = (maxDist * 16) * (maxDist * 16)

  private val reorderingTimer = TickableTimer(60)

  private def distSq(p: Pose, c: ChunkRelWorld): Double = ChunkLoadingPrioritizer.distSq(p, c)

  def +=(chunk: ChunkRelWorld): Unit = {
    edge.loadChunk(chunk)
  }

  def -=(chunk: ChunkRelWorld): Unit = {
    edge.unloadChunk(chunk)
  }

  def tick(origin: Pose): Unit = {
    this.origin = origin
    if reorderingTimer.tick() then reorderPQs()
  }

  def reorderPQs(): Unit = {
    val addSeq = addableChunks.toSeq
    addableChunks.clear()
    addableChunks.enqueue(addSeq*)

    val remSeq = removableChunks.toSeq
    removableChunks.clear()
    removableChunks.enqueue(remSeq*)
  }

  def nextAddableChunk: Option[ChunkRelWorld] = {
    while addableChunks.nonEmpty && !edge.canLoad(addableChunks.head) do {
      addableChunks.dequeue()
    }

    if addableChunks.nonEmpty then {
      Some(addableChunks.head).filter(coords => distSq(origin, coords) <= maxDistSqInBlocks)
    } else {
      Some(CoordUtils.approximateChunkCoords(origin.pos)).filter(coords => !edge.isLoaded(coords))
    }
  }

  def nextAddableChunks(n: Int): Seq[ChunkRelWorld] = {
    if n == 1 then {
      return nextAddableChunk.toSeq
    } else if n < 1 then {
      return Seq()
    }

    while addableChunks.nonEmpty && !edge.canLoad(addableChunks.head) do {
      addableChunks.dequeue()
    }

    val result: mutable.ArrayBuffer[ChunkRelWorld] = mutable.ArrayBuffer.empty

    if addableChunks.nonEmpty then {
      result ++= addableChunks.take(n)
    } else {
      val startCoords = CoordUtils.approximateChunkCoords(origin.pos)
      if !edge.isLoaded(startCoords) then {
        result += startCoords
        result ++= addableChunks.take(n - 1)
      } else {
        result ++= addableChunks.take(n)
      }
    }

    result.filter(coords => distSq(origin, coords) <= maxDistSqInBlocks).toSeq
  }

  def nextRemovableChunk: Option[ChunkRelWorld] = {
    while removableChunks.nonEmpty && !edge.onEdge(removableChunks.head) do {
      removableChunks.dequeue()
    }

    if removableChunks.nonEmpty then {
      Some(removableChunks.head).filter(coords => distSq(origin, coords) > maxDistSqInBlocks)
    } else {
      None
    }
  }

  def popChunkToLoad(): Option[ChunkRelWorld] = {
    val chunk = nextAddableChunk
    if chunk.isDefined then {
      this += chunk.get
    }
    chunk
  }

  def popChunkToRemove(): Option[ChunkRelWorld] = {
    val chunk = nextRemovableChunk
    if chunk.isDefined then {
      this -= chunk.get
    }
    chunk
  }

  private def makeChunkLoadingEdge(): ChunkLoadingEdge = {
    import ChunkLoadingEdge.Event

    val (tx, rx) = Channel[ChunkLoadingEdge.Event]()
    val edge = new ChunkLoadingEdge(tx)

    rx.onEvent {
      case Event.ChunkOnEdge(chunk, onEdge) =>
        if onEdge then {
          this.removableChunks += chunk
        }
      case Event.ChunkLoadable(chunk, loadable) =>
        if loadable then {
          this.addableChunks += chunk
        }
    }

    edge
  }
}

object ChunkLoadingEdge {
  enum Event {
    case ChunkOnEdge(chunk: ChunkRelWorld, onEdge: Boolean)
    case ChunkLoadable(chunk: ChunkRelWorld, loadable: Boolean)
  }
}

class ChunkLoadingEdge(dispatcher: Channel.Sender[ChunkLoadingEdge.Event])(using CylinderSize) {
  private val chunksLoaded: mutable.Set[ChunkRelWorld] = mutable.HashSet.empty
  private val chunksEdge: mutable.Set[ChunkRelWorld] = mutable.HashSet.empty
  private val chunksLoadable: mutable.Set[ChunkRelWorld] = mutable.HashSet.empty

  def isLoaded(chunk: ChunkRelWorld): Boolean = chunksLoaded.contains(chunk)

  def onEdge(chunk: ChunkRelWorld): Boolean = chunksEdge.contains(chunk)

  def canLoad(chunk: ChunkRelWorld): Boolean = chunksLoadable.contains(chunk)

  def loadChunk(chunk: ChunkRelWorld): Unit = {
    setLoaded(chunk, true)
    setOnEdge(chunk, true)
    setLoadable(chunk, false)

    for n <- chunk.neighbors
    do {
      if n.neighbors.forall(isLoaded) then {
        setOnEdge(n, false)
      }
      if !isLoaded(n) then {
        setLoadable(n, true)
      }
    }
  }

  def unloadChunk(chunk: ChunkRelWorld): Unit = {
    setLoaded(chunk, false)
    setOnEdge(chunk, false)
    setLoadable(chunk, chunk.neighbors.exists(isLoaded))

    for n <- chunk.neighbors
    do {
      if !n.neighbors.exists(isLoaded) then {
        setLoadable(n, false)
      }
      if isLoaded(n) then {
        setOnEdge(n, onEdge = true)
      }
    }
  }

  private def setLoaded(chunk: ChunkRelWorld, loaded: Boolean): Unit = {
    if chunksLoaded.contains(chunk) != loaded then {
      chunksLoaded(chunk) = loaded
    }
  }

  private def setOnEdge(chunk: ChunkRelWorld, onEdge: Boolean): Unit = {
    if chunksEdge.contains(chunk) != onEdge then {
      chunksEdge(chunk) = onEdge
      dispatcher.send(ChunkLoadingEdge.Event.ChunkOnEdge(chunk, onEdge))
    }
  }

  private def setLoadable(chunk: ChunkRelWorld, loadable: Boolean): Unit = {
    if chunksLoadable.contains(chunk) != loadable then {
      chunksLoadable(chunk) = loadable
      dispatcher.send(ChunkLoadingEdge.Event.ChunkLoadable(chunk, loadable))
    }
  }
}
