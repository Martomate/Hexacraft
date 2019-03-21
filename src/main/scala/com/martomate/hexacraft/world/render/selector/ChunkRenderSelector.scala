package com.martomate.hexacraft.world.render.selector

import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.render.{ChunkRenderHandler, ChunkRenderer}

abstract class ChunkRenderSelector(handler: ChunkRenderHandler) {
  def addChunk(chunk: ChunkRenderer): Unit

  def updateChunk(chunk: ChunkRenderer): Unit

  def removeChunk(chunk: ChunkRenderer): Unit

  def tick(origin: CylCoords): Unit
}

class ChunkRenderSelectorIdentity(handler: ChunkRenderHandler) extends ChunkRenderSelector(handler) {
  override def addChunk(chunk: ChunkRenderer): Unit = handler.addChunk(chunk)

  override def updateChunk(chunk: ChunkRenderer): Unit = handler.updateChunk(chunk)

  override def removeChunk(chunk: ChunkRenderer): Unit = handler.removeChunk(chunk)

  override def tick(origin: CylCoords): Unit = ()
}

class ChunkRenderSelectorNotBuried(handler: ChunkRenderHandler) extends ChunkRenderSelector(handler) {
  private def notBuried(chunk: ChunkRenderer): Boolean =
    (0 until 8).exists(from => (0 until 8).exists(to => chunk.canGetToSide(from, to)))

  override def addChunk(chunk: ChunkRenderer): Unit = if (notBuried(chunk)) handler.addChunk(chunk)

  override def updateChunk(chunk: ChunkRenderer): Unit = {
    if (notBuried(chunk))
      handler.updateChunk(chunk)
    else
      handler.removeChunk(chunk)
  }

  override def removeChunk(chunk: ChunkRenderer): Unit = handler.removeChunk(chunk)

  override def tick(origin: CylCoords): Unit = ()
}
