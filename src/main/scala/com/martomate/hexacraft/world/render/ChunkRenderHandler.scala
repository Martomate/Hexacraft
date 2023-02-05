package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.renderer.{Shaders, Texture, TextureArray}
import com.martomate.hexacraft.renderer.Shader
import com.martomate.hexacraft.world.camera.Camera
import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld
import com.martomate.hexacraft.world.render.aspect.HexagonRenderHandler

import org.joml.Vector3f
import scala.collection.mutable

class ChunkRenderHandler:
  private val hexagonHandlers: mutable.Map[Texture, HexagonRenderHandler] = mutable.Map.empty

  private val blockShader = Shader.get(Shaders.ShaderNames.Block).get
  private val blockSideShader = Shader.get(Shaders.ShaderNames.BlockSide).get
  private val blockTexture = TextureArray.getTextureArray("blocks")

  def onTotalSizeChanged(totalSize: Int): Unit =
    blockShader.setUniform1i("totalSize", totalSize)
    blockSideShader.setUniform1i("totalSize", totalSize)

  def onProjMatrixChanged(camera: Camera): Unit =
    camera.setProjMatrix(blockShader)
    camera.setProjMatrix(blockSideShader)

  def render(camera: Camera, sun: Vector3f): Unit =
    camera.updateUniforms(blockShader)
    camera.updateUniforms(blockSideShader)
    blockShader.setUniform3f("sun", sun.x, sun.y, sun.z)
    blockSideShader.setUniform3f("sun", sun.x, sun.y, sun.z)

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
