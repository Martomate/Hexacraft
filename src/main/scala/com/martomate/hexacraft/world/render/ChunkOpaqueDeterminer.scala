package com.martomate.hexacraft.world.render

trait ChunkOpaqueDeterminer {
  def canGetToSide(fromSide: Int, toSide: Int): Boolean

  def invalidate(): Unit
}
