package com.martomate.hexacraft.resource

import com.martomate.hexacraft.util.FileUtils
import javax.imageio.ImageIO
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.{GL11, GL12}

object TextureSingle {
  private val textures = collection.mutable.Map.empty[String, TextureSingle]

  private var boundTexture: TextureSingle = _

  def unbind(): Unit = {
    TextureSingle.boundTexture = null
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)
  }

  def getTexture(name: String): TextureSingle = textures.getOrElse(name, new TextureSingle(name))
}

class TextureSingle(val name: String) extends Resource {
  private var texID: Int = _
  private var texWidth: Int = _
  private var texHeight: Int = _

  def id: Int = texID
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
    for (i <- 0 until pix.size) buf.put((pix(i) >> 16).toByte).put((pix(i) >> 8).toByte).put((pix(i) >> 0).toByte).put((pix(i) >> 24).toByte)
    buf.flip
    texID = GL11.glGenTextures()
    bind()
    GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf)

    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE)
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE)
  }
  
  protected def reload(): Unit = {
    unload()
    load()
  }

  def bind(): Unit = {
    if (TextureSingle.boundTexture != this) {
      TextureSingle.boundTexture = this
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, texID)
    }
  }

  def unload(): Unit = {
    if (TextureSingle.boundTexture == this) TextureSingle.unbind()
    GL11.glDeleteTextures(texID)
  }
}
