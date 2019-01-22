package com.martomate.hexacraft.world.render

import java.nio.ByteBuffer

import com.martomate.hexacraft.resource.{Shader, Texture, TextureArray}

import scala.collection.mutable

class ChunkRenderHandler {
  private val hexagonHandlers: mutable.Map[Texture, HexagonRenderHandler] = mutable.Map.empty

  private val blockShader = Shader.get("block").get
  private val blockSideShader = Shader.get("blockSide").get
  private val blockTexture = TextureArray.getTextureArray("blocks")

  def render(): Unit = {
    for ((t, r) <- hexagonHandlers) {
      t.bind()
      r.render()
    }
  }

  def addChunk(chunk: ChunkRenderer): Unit = updateChunk(chunk)

  def updateChunk(chunk: ChunkRenderer): Unit = {
    chunk.updateContent()
    updateHandlers(chunk, Some(chunk.getRenderData))
  }

  def removeChunk(chunk: ChunkRenderer): Unit = updateHandlers(chunk, None)

  private def updateHandlers(chunk: ChunkRenderer, data: Option[ChunkRenderData]): Unit = {
    hexagonHandlers.getOrElseUpdate(blockTexture, new HexagonRenderHandler(blockShader, blockSideShader)).setChunkContent(chunk, data.map(_.blockSide))
  }

  def unload(): Unit = {
    hexagonHandlers.values.foreach(_.unload())
    hexagonHandlers.clear()
  }
}

class HexagonRenderHandler(topShader: Shader, sideShader: Shader) {

  def bufferHandlerMaker(s: Int): BufferHandler[_] = new VAOBufferHandler(new BlockVAORenderBufferFactory(s))

  private val sideHandlers: IndexedSeq[RenderAspectHandler] = IndexedSeq.tabulate(8)(s => new RenderAspectHandler(bufferHandlerMaker(s)))

  def render(): Unit = {
    for (side <- 0 until 8) {
      val sh = if (side < 2) topShader else sideShader
      sh.enable()
      sh.setUniform1i("side", side)
      sideHandlers(side).render()
    }
  }

  def setChunkContent(chunk: ChunkRenderer, content: Option[IndexedSeq[ByteBuffer]]): Unit = {
    for (s <- 0 until 8) sideHandlers(s).setChunkContent(chunk, content.flatMap(c => Option(c(s))))
  }

  def unload(): Unit = sideHandlers.foreach(_.unload())
}
