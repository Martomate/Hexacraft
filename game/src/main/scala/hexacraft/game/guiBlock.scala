package hexacraft.game

import hexacraft.gui.comp.GUITransformation
import hexacraft.infra.gpu.OpenGL
import hexacraft.renderer.*
import hexacraft.world.CameraProjection
import hexacraft.world.block.Block
import hexacraft.world.render.BlockRenderer

import org.joml.Matrix4f

object GuiBlockRenderer {
  private val guiBlockShader = new GuiBlockShader(isSide = false)
  private val guiBlockSideShader = new GuiBlockShader(isSide = true)
}

class GuiBlockRenderer(w: Int, h: Int, separation: Float = 0.2f)(blockTextureIndices: Map[String, IndexedSeq[Int]]) {
  private val guiBlockRenderers =
    for s <- 0 until 8 yield {
      BlockRenderer(GuiBlockVao.forSide(s), GpuState.of(OpenGL.State.DepthTest -> false))
    }

  private val guiBlockShader = GuiBlockRenderer.guiBlockShader
  private val guiBlockSideShader = GuiBlockRenderer.guiBlockSideShader
  private val blockTexture: TextureArray = TextureArray.getTextureArray("blocks")

  private var viewMatrix = new Matrix4f

  def setViewMatrix(matrix: Matrix4f): Unit = {
    viewMatrix = matrix
  }

  private val cameraProjection = new CameraProjection(70f, 16f / 9f, 0.02f, 1000)

  def setWindowAspectRatio(aspectRatio: Float): Unit = {
    cameraProjection.aspect = aspectRatio
    cameraProjection.updateProjMatrix()
    guiBlockShader.setWindowAspectRatio(aspectRatio)
    guiBlockSideShader.setWindowAspectRatio(aspectRatio)
  }

  def render(transformation: GUITransformation): Unit = {
    guiBlockShader.setViewMatrix(viewMatrix)
    guiBlockSideShader.setViewMatrix(viewMatrix)

    guiBlockShader.setProjectionMatrix(cameraProjection.matrix)
    guiBlockSideShader.setProjectionMatrix(cameraProjection.matrix)

    blockTexture.bind()

    for side <- 0 until 8 do {
      val sh = if side < 2 then guiBlockShader else guiBlockSideShader
      sh.enable()
      sh.setSide(side)
      guiBlockRenderers(side).render()
    }
  }

  def updateContent(xOff: Float, yOff: Float, blocks: Seq[Block]): Unit = {
    for side <- 0 until 8 do {
      guiBlockRenderers(side).setInstanceData(9 * 9): buf =>
        for y <- 0 until h do {
          for x <- 0 until w do {
            val blockToDraw = blocks(x + y * w)
            if blockToDraw.canBeRendered then {
              buf.putFloat(x * separation + xOff)
              buf.putFloat(y * separation + yOff)
              buf.putInt(blockTextureIndices(blockToDraw.name)(side))
              buf.putFloat(1)
              buf.putFloat(1)
            }
          }
        }
    }
  }

  def unload(): Unit = {
    for r <- guiBlockRenderers do {
      r.unload()
    }
  }
}

class GuiBlockShader(isSide: Boolean) {
  private val config = ShaderConfig("gui_block")
    .withInputs(
      "position",
      "texCoords",
      "normal",
      "vertexIndex",
      "faceIndex",
      "blockPos",
      "blockTex",
      "blockHeight",
      "brightness"
    )
    .withDefines("isSide" -> (if isSide then "1" else "0"))

  private val shader = Shader.from(config)

  def setWindowAspectRatio(aspectRatio: Float): Unit = {
    shader.setUniform1f("windowAspectRatio", aspectRatio)
  }

  def setProjectionMatrix(matrix: Matrix4f): Unit = {
    shader.setUniformMat4("projMatrix", matrix)
  }

  def setViewMatrix(matrix: Matrix4f): Unit = {
    shader.setUniformMat4("viewMatrix", matrix)
  }

  def setSide(side: Int): Unit = {
    shader.setUniform1i("side", side)
  }

  def enable(): Unit = {
    shader.activate()
  }
}

object GuiBlockVao {
  def forSide(side: Int): VAO = {
    VAO
      .builder()
      .addVertexVbo(BlockRenderer.verticesPerInstance(side), OpenGL.VboUsage.StaticDraw)(
        _.floats(0, 3)
          .floats(1, 2)
          .floats(2, 3)
          .ints(3, 1)
          .ints(4, 1),
        _.fill(0, BlockRenderer.setupBlockVBO(side))
      )
      .addInstanceVbo(0, OpenGL.VboUsage.DynamicDraw)(
        _.floats(5, 2)
          .ints(6, 1)
          .floats(7, 1)
          .floats(8, 1)
      )
      .finish(BlockRenderer.verticesPerInstance(side), 0)
  }
}
