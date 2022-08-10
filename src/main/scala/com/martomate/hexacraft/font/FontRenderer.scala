package com.martomate.hexacraft.font

import com.martomate.hexacraft.renderer.VAO
import com.martomate.hexacraft.resource.{Shaders, TextureSingle}
import com.martomate.hexacraft.font.mesh.{FontType, GUIText}
import org.lwjgl.opengl.{GL11, GL13, GL30}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class FontRenderer {
  private val shader = Shaders.Font

  def render(
      texts: mutable.HashMap[FontType, ArrayBuffer[GUIText]],
      xoffset: Float,
      yoffset: Float
  ): Unit = {
    prepare()
    for (font <- texts.keys) {
      GL13.glActiveTexture(GL13.GL_TEXTURE0)
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, font.getTextureAtlas)
      for (text <- texts(font)) {
        renderText(text, xoffset, yoffset)
      }
    }
    endRendering()
  }

  private def prepare(): Unit = {
    GL11.glEnable(GL11.GL_BLEND)
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
    GL11.glDisable(GL11.GL_DEPTH_TEST)
    shader.enable()
    TextureSingle.unbind()
    VAO.unbindVAO()
  }

  private def renderText(text: GUIText, xoffset: Float, yoffset: Float): Unit = {
    GL30.glBindVertexArray(text.getMesh)
    shader.setUniform3f("color", text.getColour)
    shader.setUniform2f("translation", text.getPosition.x + xoffset, text.getPosition.y + yoffset)
    GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, text.getVertexCount)
  }

  private def endRendering(): Unit = {
    GL11.glDisable(GL11.GL_BLEND)
    GL11.glEnable(GL11.GL_DEPTH_TEST)
    TextureSingle.unbind() // important
    VAO.unbindVAO() // important
  }
}
