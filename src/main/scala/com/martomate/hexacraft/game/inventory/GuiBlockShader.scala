package com.martomate.hexacraft.game.inventory

import com.martomate.hexacraft.renderer.{Shader, Shaders}

import org.joml.Matrix4f

class GuiBlockShader(isSide: Boolean) {
  private val shader: Shader =
    Shader.get(if isSide then Shaders.ShaderNames.GuiBlockSide else Shaders.ShaderNames.GuiBlock).get

  def setProjectionMatrix(matrix: Matrix4f): Unit =
    shader.setUniformMat4("projMatrix", matrix)

  def setViewMatrix(matrix: Matrix4f): Unit =
    shader.setUniformMat4("viewMatrix", matrix)

  def setSide(side: Int): Unit =
    shader.setUniform1i("side", side)

  def enable(): Unit = shader.enable()
}
