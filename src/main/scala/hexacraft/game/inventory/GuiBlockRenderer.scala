package hexacraft.game.inventory

import hexacraft.gui.comp.GUITransformation
import hexacraft.infra.gpu.OpenGL
import hexacraft.renderer.{GpuState, InstancedRenderer, TextureArray}
import hexacraft.world.block.{Block, BlockSpecRegistry}
import hexacraft.world.camera.CameraProjection
import hexacraft.world.render.{BlockRenderer, BlockRendererCollection}

import org.joml.Matrix4f

object GuiBlockRenderer:
  private val guiBlockShader = new GuiBlockShader(isSide = false)
  private val guiBlockSideShader = new GuiBlockShader(isSide = true)

class GuiBlockRenderer(w: Int, h: Int, separation: Float = 0.2f)(using blockSpecs: BlockSpecRegistry):
  private val guiBlockRenderer = BlockRendererCollection: s =>
    val vao = GuiBlockVao.forSide(s)
    val renderer = InstancedRenderer(OpenGL.PrimitiveMode.Triangles, GpuState.of(OpenGL.State.DepthTest -> false))
    BlockRenderer(s, vao, renderer)

  private val guiBlockShader = GuiBlockRenderer.guiBlockShader
  private val guiBlockSideShader = GuiBlockRenderer.guiBlockSideShader
  private val blockTexture: TextureArray = TextureArray.getTextureArray("blocks")

  private var viewMatrix = new Matrix4f
  def setViewMatrix(matrix: Matrix4f): Unit = viewMatrix = matrix

  private val cameraProjection = new CameraProjection(70f, 16f / 9f, 0.02f, 1000)

  def setWindowAspectRatio(aspectRatio: Float): Unit =
    cameraProjection.aspect = aspectRatio
    cameraProjection.updateProjMatrix()
    guiBlockShader.setWindowAspectRatio(aspectRatio)
    guiBlockSideShader.setWindowAspectRatio(aspectRatio)

  def render(transformation: GUITransformation): Unit =
    guiBlockShader.setViewMatrix(viewMatrix)
    guiBlockSideShader.setViewMatrix(viewMatrix)

    guiBlockShader.setProjectionMatrix(cameraProjection.matrix)
    guiBlockSideShader.setProjectionMatrix(cameraProjection.matrix)

    blockTexture.bind()

    for (side <- 0 until 8)
      val sh = if side < 2 then guiBlockShader else guiBlockSideShader
      sh.enable()
      sh.setSide(side)
      guiBlockRenderer.renderBlockSide(side)

  def updateContent(xOff: Float, yOff: Float, blocks: Seq[Block]): Unit =
    for side <- 0 until 8 do
      guiBlockRenderer.updateContent(side, 9 * 9): buf =>
        for y <- 0 until h do
          for x <- 0 until w do
            val blockToDraw = blocks(x + y * w)
            if blockToDraw.canBeRendered
            then
              buf.putFloat(x * separation + xOff)
              buf.putFloat(y * separation + yOff)
              buf.putInt(blockSpecs.textureIndex(blockToDraw.name, side))
              buf.putFloat(1)
              buf.putFloat(1)

  def unload(): Unit =
    guiBlockRenderer.unload()
