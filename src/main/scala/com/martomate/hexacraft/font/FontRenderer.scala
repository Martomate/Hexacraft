package com.martomate.hexacraft.font

import com.martomate.hexacraft.font.mesh.{FontType, GUIText}
import com.martomate.hexacraft.renderer.{Shaders, TextureSingle, VAO}
import com.martomate.hexacraft.renderer.Shader
import com.martomate.hexacraft.util.OpenGL

import org.lwjgl.opengl.{GL11, GL13}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class FontRenderer {
  private val shader = Shader.get(Shaders.ShaderNames.Font).get

  def render(
      texts: mutable.HashMap[FontType, ArrayBuffer[GUIText]],
      xoffset: Float,
      yoffset: Float
  ): Unit = {
    prepare()
    for (font <- texts.keys) {
      OpenGL.glActiveTexture(GL13.GL_TEXTURE0)
      OpenGL.glBindTexture(GL11.GL_TEXTURE_2D, font.textureAtlas)
      for (text <- texts(font)) {
        renderText(text, xoffset, yoffset)
      }
    }
    endRendering()
  }

  private def prepare(): Unit = {
    OpenGL.glEnable(GL11.GL_BLEND)
    OpenGL.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
    OpenGL.glDisable(GL11.GL_DEPTH_TEST)
    shader.enable()
    TextureSingle.unbind()
    VAO.unbindVAO()
  }

  private def renderText(text: GUIText, xoffset: Float, yoffset: Float): Unit = {
    OpenGL.glBindVertexArray(text.getMesh)
    shader.setUniform3f("color", text.color)
    shader.setUniform2f("translation", text.position.x + xoffset, text.position.y + yoffset)
    OpenGL.glDrawArrays(GL11.GL_TRIANGLES, 0, text.vertexCount)
  }

  private def endRendering(): Unit = {
    OpenGL.glDisable(GL11.GL_BLEND)
    OpenGL.glEnable(GL11.GL_DEPTH_TEST)
    TextureSingle.unbind() // important
    VAO.unbindVAO() // important
  }
}
