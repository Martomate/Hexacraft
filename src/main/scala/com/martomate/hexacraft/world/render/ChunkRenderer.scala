package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld

trait ChunkRenderer {
  def coords: ChunkRelWorld

  def canGetToSide(fromSide: Int, toSide: Int): Boolean

  def getRenderData: ChunkRenderData

  def updateContent(): Unit

  def appendEntityRenderData(side: Int, append: EntityDataForShader => Unit): Unit

  def unload(): Unit
}
