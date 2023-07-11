package com.martomate.hexacraft.font

import com.martomate.hexacraft.font.mesh.{FontType, GUIText}
import com.martomate.hexacraft.infra.gpu.OpenGL
import com.martomate.hexacraft.renderer.{TextureSingle, VAO}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object FontRenderer {
  private val shader = new FontShader()
}

class FontRenderer {
  private val shader = FontRenderer.shader

  def render(
      texts: mutable.HashMap[FontType, ArrayBuffer[GUIText]],
      xoffset: Float,
      yoffset: Float
  ): Unit = {
    prepare()
    for (font <- texts.keys) {
      OpenGL.glActiveTexture(OpenGL.TextureSlot.ofSlot(0))
      OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2D, font.textureAtlas)
      for (text <- texts(font)) {
        renderText(text, xoffset, yoffset)
      }
    }
    endRendering()
  }

  private def prepare(): Unit = {
    OpenGL.glEnable(OpenGL.State.Blend)
    OpenGL.glBlendFunc(OpenGL.BlendFactor.SrcAlpha, OpenGL.BlendFactor.OneMinusSrcAlpha)
    OpenGL.glDisable(OpenGL.State.DepthTest)
    shader.enable()
    TextureSingle.unbind()
    VAO.unbindVAO()
  }

  private def renderText(text: GUIText, xoffset: Float, yoffset: Float): Unit = {
    OpenGL.glBindVertexArray(text.getMesh)
    shader.setColor(text.color)
    shader.setTranslation(text.position.x + xoffset, text.position.y + yoffset)
    OpenGL.glDrawArrays(OpenGL.PrimitiveMode.Triangles, 0, text.vertexCount)
  }

  private def endRendering(): Unit = {
    OpenGL.glDisable(OpenGL.State.Blend)
    OpenGL.glEnable(OpenGL.State.DepthTest)
    TextureSingle.unbind() // important
    VAO.unbindVAO() // important
  }
}
