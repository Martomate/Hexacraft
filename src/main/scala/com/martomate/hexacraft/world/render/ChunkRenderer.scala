package com.martomate.hexacraft.world.render

trait ChunkRenderer {
  def canGetToSide(fromSide: Int, toSide: Int): Boolean

  def getRenderData: ChunkRenderData

  def updateContent(): Unit

  def appendEntityRenderData(side: Int, append: EntityDataForShader => Unit): Unit

  def unload(): Unit
}
