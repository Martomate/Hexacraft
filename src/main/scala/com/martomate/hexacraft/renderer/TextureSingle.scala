package com.martomate.hexacraft.renderer

import com.martomate.hexacraft.util.{FileUtils, OpenGL, Resource}
import com.martomate.hexacraft.util.OpenGL.{
  TexelDataFormat,
  TexelDataType,
  TextureId,
  TextureInternalFormat,
  TextureTarget
}

import javax.imageio.ImageIO
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.{GL11, GL12}

object TextureSingle {
  private val textures = collection.mutable.Map.empty[String, TextureSingle]

  private var boundTexture: TextureSingle = _

  def unbind(): Unit = {
    TextureSingle.boundTexture = null
    OpenGL.glBindTexture(TextureTarget.Texture2D, TextureId.none)
  }

  def getTexture(name: String): TextureSingle = textures.getOrElse(name, new TextureSingle(name))
}

class TextureSingle(val name: String) extends Resource with Texture {
  private var texID: TextureId = _
  private var texWidth: Int = _
  private var texHeight: Int = _

  def id: TextureId = texID
  def width: Int = texWidth
  def height: Int = texHeight

  TextureSingle.textures += name -> this

  load()

  def load(): Unit = {
    val image = ImageIO.read(FileUtils.getResourceFile(name + ".png").get)
    val width = image.getWidth
    val height = image.getHeight
    texWidth = width
    texHeight = height
    val pix = image.getRGB(0, 0, width, height, null, 0, width)
    val buf = BufferUtils.createByteBuffer(pix.length * 4)
    for (i <- 0 until pix.size)
      buf
        .put((pix(i) >> 16).toByte)
        .put((pix(i) >> 8).toByte)
        .put((pix(i) >> 0).toByte)
        .put((pix(i) >> 24).toByte)
    buf.flip
    texID = OpenGL.glGenTextures()
    bind()
    OpenGL.glTexImage2D(
      TextureTarget.Texture2D,
      0,
      TextureInternalFormat.Rgba,
      width,
      height,
      0,
      TexelDataFormat.Rgba,
      TexelDataType.UnsignedByte,
      buf
    )

    OpenGL.glTexParameteri(TextureTarget.Texture2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
    OpenGL.glTexParameteri(TextureTarget.Texture2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
    OpenGL.glTexParameteri(TextureTarget.Texture2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE)
    OpenGL.glTexParameteri(TextureTarget.Texture2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE)
  }

  protected def reload(): Unit = {
    unload()
    load()
  }

  def bind(): Unit = {
    if (TextureSingle.boundTexture != this) {
      TextureSingle.boundTexture = this
      OpenGL.glBindTexture(TextureTarget.Texture2D, texID)
    }
  }

  def unload(): Unit = {
    if (TextureSingle.boundTexture == this) TextureSingle.unbind()
    OpenGL.glDeleteTextures(texID)
  }
}
