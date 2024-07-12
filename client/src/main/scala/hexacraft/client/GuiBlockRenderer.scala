package hexacraft.client

import hexacraft.gui.comp.GUITransformation
import hexacraft.renderer.*
import hexacraft.shaders.GuiBlockShader
import hexacraft.world.CameraProjection
import hexacraft.world.block.Block
import hexacraft.world.render.BlockRenderer

import org.joml.{Matrix4f, Vector2f}

import scala.collection.mutable

object GuiBlockRenderer {
  private val guiBlockShader = new GuiBlockShader(isSide = false)
  private val guiBlockSideShader = new GuiBlockShader(isSide = true)
}

class GuiBlockRenderer(w: Int, h: Int, separation: Float = 0.2f)(blockTextureIndices: Map[String, IndexedSeq[Int]]) {
  private val guiBlockRenderers =
    for s <- 0 until 8 yield {
      BlockRenderer(
        GuiBlockShader.createVao(s),
        GpuState.build(_.blend(true).depthTest(false))
      )
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
      val data = new mutable.ArrayBuffer[GuiBlockShader.InstanceData](h * w)

      for y <- 0 until h do {
        for x <- 0 until w do {
          val blockToDraw = blocks(x + y * w)
          if blockToDraw.canBeRendered then {
            val blockPos = Vector2f(x * separation + xOff, y * separation + yOff)
            val blockTex = blockTextureIndices(blockToDraw.name)(side)
            data += GuiBlockShader.InstanceData(blockPos, blockTex, 1, 1)
          }
        }
      }

      guiBlockRenderers(side).setInstanceData(data.size): buf =>
        data.foreach(_.fill(buf))
    }
  }

  def unload(): Unit = {
    for r <- guiBlockRenderers do {
      r.unload()
    }
  }
}
