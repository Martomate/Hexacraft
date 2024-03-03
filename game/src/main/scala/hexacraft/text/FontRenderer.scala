package hexacraft.text

import hexacraft.infra.gpu.OpenGL
import hexacraft.renderer.{TextureSingle, VAO}
import hexacraft.shaders.font.FontShader
import hexacraft.text.font.Font

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object FontRenderer {
  private val shader = new FontShader()
}

class FontRenderer {
  private val shader = FontRenderer.shader

  def setWindowAspectRatio(aspectRatio: Float): Unit = {
    shader.setWindowAspectRatio(aspectRatio)
  }

  def render(
      texts: mutable.HashMap[Font, ArrayBuffer[Text]],
      xOffset: Float,
      yOffset: Float
  ): Unit = {
    prepare()
    for font <- texts.keys do {
      OpenGL.glActiveTexture(OpenGL.TextureSlot.ofSlot(0))
      OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2D, font.textureAtlas)
      for text <- texts(font) do {
        renderText(text, xOffset, yOffset)
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

  private def renderText(text: Text, xOffset: Float, yOffset: Float): Unit = {
    OpenGL.glBindVertexArray(text.getMesh)

    if text.hasShadow then {
      shader.setColor(text.shadowColor)
      shader.setTranslation(
        text.position.x + xOffset + 0.001f * (2 + text.fontSize * 0.5f),
        text.position.y + yOffset + -0.001f * (2 + text.fontSize * 0.5f)
      )
      OpenGL.glDrawArrays(OpenGL.PrimitiveMode.Triangles, 0, text.vertexCount)
    }

    shader.setColor(text.color)
    shader.setTranslation(text.position.x + xOffset, text.position.y + yOffset)
    OpenGL.glDrawArrays(OpenGL.PrimitiveMode.Triangles, 0, text.vertexCount)
  }

  private def endRendering(): Unit = {
    OpenGL.glDisable(OpenGL.State.Blend)
    OpenGL.glEnable(OpenGL.State.DepthTest)
    TextureSingle.unbind() // important
    VAO.unbindVAO() // important
  }
}
