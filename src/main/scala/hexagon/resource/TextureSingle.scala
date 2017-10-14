package hexagon.resource

import java.io.File
import javax.imageio.ImageIO
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.EXTTextureFilterAnisotropic
import org.lwjgl.opengl.GL

object TextureSingle {
  private val textures = collection.mutable.Map.empty[String, TextureSingle]

  private var boundTexture: TextureSingle = _

  def unbind(): Unit = {
    TextureSingle.boundTexture = null
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)
  }

  def getTexture(name: String): TextureSingle = textures.get(name).getOrElse(new TextureSingle(name))
}

class TextureSingle(val name: String) extends Resource {
  private var texID: Int = _

  TextureSingle.textures += name -> this

  load()

  def load(): Unit = {
    val image = ImageIO.read(new File("res/" + name + ".png"))
    val width = image.getWidth
    val height = image.getHeight
    val pix = image.getRGB(0, 0, width, height, null, 0, width)
    val buf = BufferUtils.createByteBuffer(pix.length * 4)
    for (i <- 0 until pix.size) buf.put((pix(i) >> 16).toByte).put((pix(i) >> 8).toByte).put((pix(i) >> 0).toByte).put((pix(i) >> 24).toByte)
    buf.flip
    texID = GL11.glGenTextures()
    bind()
    GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf)
    if (GL.getCapabilities().GL_EXT_texture_filter_anisotropic) {
      val amt = Math.min(16f, GL11.glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT));
      GL11.glTexParameterf(GL11.GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, amt);
    } else {
      println("Anisotropic filtering is not supported :(");
    }
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST) // _MIPMAP_LINEAR)
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE)
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE)
    GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D)
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
    if (TextureSingle.boundTexture == this) TextureSingle.unbind();
    GL11.glDeleteTextures(texID)
  }
}
