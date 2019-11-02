package com.martomate.hexacraft.world.loader

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.coord.CoordUtils
import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld

import scala.collection.mutable

class ChunkLoadingPrioritizerSimple(origin: PosAndDir,
                                    distSqFunc: (PosAndDir, ChunkRelWorld) => Double,
                                    maxDist: Double
                                   )(implicit cylSize: CylinderSize) extends ChunkLoadingPrioritizer {

  private val chunksLoaded: mutable.Set[ChunkRelWorld] = new mutable.HashSet
  private val chunksLoadingEdge: mutable.Set[ChunkRelWorld] = new mutable.HashSet

  private val maxDistSqInBlocks: Double = (maxDist * 16) * (maxDist * 16)

  override def +=(chunk: ChunkRelWorld): Unit = {
    chunksLoaded += chunk
    chunksLoadingEdge += chunk
    removeNonEdgeNeighbors(chunk)
    assert(chunksLoadingEdge contains chunk)
  }

  private def removeNonEdgeNeighbors(chunk: ChunkRelWorld): Unit = {
    chunk.neighbors.foreach(neigh => removeIfNotEdge(neigh))
  }

  private def removeIfNotEdge(chunk: ChunkRelWorld): Unit = {
    if (chunk.neighbors.forall(neigh => chunksLoaded(neigh))) chunksLoadingEdge -= chunk
  }

  override def -=(chunk: ChunkRelWorld): Unit = {
    chunksLoaded -= chunk
    chunksLoadingEdge -= chunk
    addNewEdgeNeighbors(chunk)
  }

  private def addNewEdgeNeighbors(chunk: ChunkRelWorld): Unit = {
    chunk.neighbors.foreach(n => if (chunksLoaded(n)) chunksLoadingEdge += n)
  }

  override def tick(): Unit = ()

  override def nextAddableChunk: Option[ChunkRelWorld] = {
    if (chunksLoadingEdge.isEmpty) {
      val coords = CoordUtils.approximateChunkCoords(origin.pos)(origin.pos.cylSize)
      if (!chunksLoaded(coords)) Some(coords) else None
    } else {
      chunksLoadingEdge.toStream.map(edge => edge.neighbors.find(
        coords => !chunksLoaded(coords) && distSqFunc(origin, coords) <= maxDistSqInBlocks
      )).find(_.isDefined).flatten
    }
  }

  override def nextRemovableChunk: Option[ChunkRelWorld] =
    chunksLoadingEdge.find(coords => distSqFunc(origin, coords) > maxDistSqInBlocks)

  override def unload(): Unit = ()
}
