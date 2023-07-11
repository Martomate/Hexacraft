package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.renderer.{Shader, ShaderConfig}

import org.joml.{Matrix4f, Vector3d, Vector3f}

class EntityShader(isSide: Boolean) {
  private val config = ShaderConfig("entity_part")
    .withAttribs(
      "position",
      "texCoords",
      "normal",
      "vertexIndex",
      "faceIndex",
      "modelMatrix",
      "",
      "",
      "",
      "texOffset",
      "texDim",
      "blockTex",
      "brightness"
    )
    .withDefines("isSide" -> (if isSide then "1" else "0"))

  private val shader = Shader.from(config)

  def setTotalSize(totalSize: Int): Unit =
    shader.setUniform1i("totalSize", totalSize)

  def setSunPosition(sun: Vector3f): Unit =
    shader.setUniform3f("sun", sun.x, sun.y, sun.z)

  def setCameraPosition(cam: Vector3d): Unit =
    shader.setUniform3f("cam", cam.x.toFloat, cam.y.toFloat, cam.z.toFloat)

  def setProjectionMatrix(matrix: Matrix4f): Unit =
    shader.setUniformMat4("projMatrix", matrix)

  def setViewMatrix(matrix: Matrix4f): Unit =
    shader.setUniformMat4("viewMatrix", matrix)

  def setSide(side: Int): Unit =
    shader.setUniform1i("side", side)

  def setTextureSize(texSize: Int): Unit =
    shader.setUniform1i("texSize", texSize)

  def enable(): Unit = shader.enable()
}
