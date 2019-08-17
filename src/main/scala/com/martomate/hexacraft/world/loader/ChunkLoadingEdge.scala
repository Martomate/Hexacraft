package com.martomate.hexacraft.world.loader

import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class ChunkLoadingEdge {
  private val chunksLoaded: mutable.Set[ChunkRelWorld] = mutable.HashSet.empty
  private val chunksEdge: mutable.Set[ChunkRelWorld] = mutable.HashSet.empty
  private val chunksLoadable: mutable.Set[ChunkRelWorld] = mutable.HashSet.empty

  private val listeners: ArrayBuffer[ChunkLoadingEdgeListener] = ArrayBuffer.empty
  def addListener(listener: ChunkLoadingEdgeListener): Unit = listeners += listener
  def removeListener(listener: ChunkLoadingEdgeListener): Unit = listeners -= listener


  def isLoaded(chunk: ChunkRelWorld): Boolean = chunksLoaded.contains(chunk)

  def onEdge(chunk: ChunkRelWorld): Boolean = chunksEdge.contains(chunk)

  def canLoad(chunk: ChunkRelWorld): Boolean = chunksLoadable.contains(chunk)

  def loadChunk(chunk: ChunkRelWorld): Unit = {
    setLoaded(chunk, loaded = true)
    setOnEdge(chunk, onEdge = true)
    setLoadable(chunk, loadable = false)
    removeNonEdgeNeighbors(chunk)
    addLoadableNeighbors(chunk)
  }

  def unloadChunk(chunk: ChunkRelWorld): Unit = {
    setLoaded(chunk, loaded = false)
    setOnEdge(chunk, onEdge = false)
    setLoadable(chunk, !shouldNotBeLoadable(chunk))
    addNewEdgeNeighbors(chunk)
    removeNonLoadableNeighbors(chunk)
  }

  private def addLoadableNeighbors(chunk: ChunkRelWorld): Unit = {
    for (n <- chunk.neighbors) {
      if (!isLoaded(n)) setLoadable(n, loadable = true)
    }
  }

  private def removeNonLoadableNeighbors(chunk: ChunkRelWorld): Unit = {
    chunk.neighbors.foreach(removeIfNotLoadable)
  }

  private def removeIfNotLoadable(chunk: ChunkRelWorld): Unit = {
    if (shouldNotBeLoadable(chunk)) setLoadable(chunk, loadable = false)
  }

  private def shouldNotBeLoadable(chunk: ChunkRelWorld) = {
    chunk.neighbors.forall(n => !isLoaded(n))
  }

  private def removeNonEdgeNeighbors(chunk: ChunkRelWorld): Unit = {
    chunk.neighbors.foreach(neigh => removeIfNotEdge(neigh))
  }

  private def removeIfNotEdge(chunk: ChunkRelWorld): Unit = {
    if (chunk.neighbors.forall(neigh => isLoaded(neigh))) setOnEdge(chunk, onEdge = false)
  }

  private def addNewEdgeNeighbors(chunk: ChunkRelWorld): Unit = {
    chunk.neighbors.foreach(n => if (isLoaded(n)) setOnEdge(n, onEdge = true))
  }

  private def setLoaded(chunk: ChunkRelWorld, loaded: Boolean): Unit = {
    if (chunksLoaded.contains(chunk) != loaded) {
      chunksLoaded(chunk) = loaded
    }
  }

  private def setOnEdge(chunk: ChunkRelWorld, onEdge: Boolean): Unit = {
    if (chunksEdge.contains(chunk) != onEdge) {
      chunksEdge(chunk) = onEdge

      listeners.foreach(_.onChunkOnEdge(chunk, onEdge))
    }
  }

  private def setLoadable(chunk: ChunkRelWorld, loadable: Boolean): Unit = {
    if (chunksLoadable.contains(chunk) != loadable) {
      chunksLoadable(chunk) = loadable

      listeners.foreach(_.onChunkLoadable(chunk, loadable))
    }
  }
}
