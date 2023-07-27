package com.martomate.hexacraft.renderer

import com.martomate.hexacraft.infra.fs.FileUtils
import com.martomate.hexacraft.infra.gpu.OpenGL
import com.martomate.hexacraft.infra.gpu.OpenGL.*
import com.martomate.hexacraft.util.Resource
import org.lwjgl.BufferUtils

import javax.imageio.ImageIO

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

    OpenGL.glTexParameteri(TextureTarget.Texture2D, TexIntParameter.MinFilter(TexMinFilter.Linear))
    OpenGL.glTexParameteri(TextureTarget.Texture2D, TexIntParameter.MagFilter(TexMagFilter.Nearest))
    OpenGL.glTexParameteri(TextureTarget.Texture2D, TexIntParameter.TextureWrapS(TexWrap.ClampToEdge))
    OpenGL.glTexParameteri(TextureTarget.Texture2D, TexIntParameter.TextureWrapT(TexWrap.ClampToEdge))
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
