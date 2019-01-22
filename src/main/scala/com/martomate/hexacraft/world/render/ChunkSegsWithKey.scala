package com.martomate.hexacraft.world.render

import scala.collection.mutable

class ChunkSegsWithKey[T] extends ChunkSegs {
  private var currentKey: T = _

  private val invMap: mutable.TreeMap[Segment, T] = mutable.TreeMap.empty(_.start - _.start)

  override protected def _add(seg: Segment): Unit = {
    super._add(seg)
    invMap.put(seg, currentKey)
  }

  override protected def _remove(seg: Segment): Unit = {
    super._remove(seg)
    invMap.remove(seg)
  }

  def add(chunk: T, segment: Segment): Unit = {
    currentKey = chunk
    add(segment)
  }

  def remove(chunk: T, segment: Segment): Unit = {
    require(invMap.find(_._1.contains(segment)).exists(_._2 == chunk))
    currentKey = chunk
    remove(segment)
  }

  def lastKeyAndSegment(): (T, Segment) = {
    val seg = lastSegment()
    (invMap(seg), seg)
  }
}
