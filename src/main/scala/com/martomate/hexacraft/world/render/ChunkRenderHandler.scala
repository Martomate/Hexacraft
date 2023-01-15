package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.renderer.{Shaders, Texture, TextureArray}
import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld
import com.martomate.hexacraft.world.render.aspect.HexagonRenderHandler

import scala.collection.mutable

class ChunkRenderHandler:
  private val hexagonHandlers: mutable.Map[Texture, HexagonRenderHandler] = mutable.Map.empty

  private val blockShader = Shaders.Block
  private val blockSideShader = Shaders.BlockSide
  private val blockTexture = TextureArray.getTextureArray("blocks")

  def render(): Unit =
    for (t, r) <- hexagonHandlers do
      t.bind()
      r.render()

  def updateHandlers(coords: ChunkRelWorld, data: Option[ChunkRenderData]): Unit =
    hexagonHandlers
      .getOrElseUpdate(blockTexture, new HexagonRenderHandler(blockShader, blockSideShader))
      .setChunkContent(coords, data.map(_.blockSide))

  def unload(): Unit =
    hexagonHandlers.values.foreach(_.unload())
    hexagonHandlers.clear()
