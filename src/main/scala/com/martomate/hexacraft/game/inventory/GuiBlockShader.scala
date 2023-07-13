package com.martomate.hexacraft.game.inventory

import com.martomate.hexacraft.renderer.{Shader, ShaderConfig}

import org.joml.{Matrix4f, Vector2i}

class GuiBlockShader(isSide: Boolean) {
  private val config = ShaderConfig("gui_block")
    .withAttribs(
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

  def setWindowAspectRatio(aspectRatio: Float): Unit =
    shader.setUniform1f("windowAspectRatio", aspectRatio)

  def setProjectionMatrix(matrix: Matrix4f): Unit =
    shader.setUniformMat4("projMatrix", matrix)

  def setViewMatrix(matrix: Matrix4f): Unit =
    shader.setUniformMat4("viewMatrix", matrix)

  def setSide(side: Int): Unit =
    shader.setUniform1i("side", side)

  def enable(): Unit = shader.activate()
}
