package com.martomate.hexacraft.game.inventory

import com.martomate.hexacraft.gui.comp.GUITransformation
import com.martomate.hexacraft.renderer.{Shader, Shaders, TextureArray}
import com.martomate.hexacraft.world.block.Block
import com.martomate.hexacraft.world.render.{BlockRendererCollection, FlatBlockRenderer}
import org.joml.{Matrix4f, Vector3f}

object GUIBlocksRenderer:
  def withSingleSlot(
      blockProvider: () => Block,
      rendererLocation: () => (Float, Float) = () => (0, 0),
      brightnessFunc: () => Float = () => 1.0f
  ): GUIBlocksRenderer =
    new GUIBlocksRenderer(1, 1)(_ => blockProvider(), rendererLocation, (_, _) => brightnessFunc())

class GUIBlocksRenderer(w: Int, h: Int = 1, separation: Float = 0.2f)(
    blockProvider: Int => Block,
    rendererLocation: () => (Float, Float) = () => (0, 0),
    brightnessFunc: (Int, Int) => Float = (_, _) => 1.0f
):
  private val guiBlockRenderer = new BlockRendererCollection(s => new FlatBlockRenderer(s, 0))
  private val guiBlockShader: Shader = Shaders.GuiBlock
  private val guiBlockSideShader: Shader = Shaders.GuiBlockSide
  private val blockTexture: TextureArray = TextureArray.getTextureArray("blocks")

  private var viewMatrix = new Matrix4f
  def setViewMatrix(matrix: Matrix4f): Unit = viewMatrix = matrix

  updateContent()

  def render(transformation: GUITransformation): Unit =
    guiBlockShader.setUniformMat4("viewMatrix", viewMatrix)
    guiBlockSideShader.setUniformMat4("viewMatrix", viewMatrix)
    blockTexture.bind()

    for (side <- 0 until 8)
      val sh = if side < 2 then guiBlockShader else guiBlockSideShader
      sh.enable()
      sh.setUniform1i("side", side)
      guiBlockRenderer.renderBlockSide(side)

  def updateContent(): Unit =
    val (xOff, yOff) = rendererLocation()

    for (side <- 0 until 8)
      guiBlockRenderer.updateContent(side, 9 * 9) { buf =>
        for (y <- 0 until h)
          for (x <- 0 until w)
            val blockToDraw = blockProvider(x + y * w)
            if blockToDraw.canBeRendered
            then
              buf.putFloat(x * separation + xOff)
              buf.putFloat(y * separation + yOff)
              buf.putInt(blockToDraw.blockTex(side))
              buf.putFloat(
                1.0f
              ) // blockInHand.blockHeight(new BlockState(BlockRelWorld(0, 0, 0, world), blockInHand)))
              buf.putFloat(1) // brightnessFunc(x, y))
      }

  def unload(): Unit =
    guiBlockRenderer.unload()
