package com.martomate.hexacraft.world.render

case class Segment(start: Int, length: Int) {
  require(start >= 0,  "'start' has to be non-negative")
  require(length > 0, "'length' has to be positive")

  def contains(inner: Segment): Boolean = inner.start >= start && inner.start + inner.length <= start + length

  def overlaps(other: Segment): Boolean = other.start < start + length && other.start + other.length > start
}
