package hexagon.resource

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.EXTTextureFilterAnisotropic
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import org.lwjgl.opengl.GL30

object TextureArray {
  private val textures = collection.mutable.Map.empty[String, TextureArray]

  private var boundTextureArray: TextureArray = _
  
  def unbind(): Unit = {
    TextureArray.boundTextureArray = null
    GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0)
  }

  def getTextureArray(name: String): TextureArray = textures.get(name).get
  
  def registerTextureArray(name: String, texSize: Int, images: ResourceWrapper[Seq[Array[Int]]]): TextureArray = {
    if (!textures.contains(name)) new TextureArray(name, texSize, images)
    else textures(name)
  }
}

class TextureArray(val name: String, val texSize: Int, wrappedImages: ResourceWrapper[Seq[Array[Int]]]) extends Resource {
  private var texID: Int = _

  TextureArray.textures += name -> this

  load()

  protected def load(): Unit = {
    val images = wrappedImages.get
    val height = texSize
    val width = texSize * images.length
    val buf = BufferUtils.createByteBuffer(height * width * 4)
    for (x <- 0 until images.length) {
      val pix = images(x)
      for (j <- 0 until texSize) {
        for (i <- 0 until texSize) {
          val idx = i + j * texSize
          buf.put((pix(idx) >> 16).toByte).put((pix(idx) >> 8).toByte).put((pix(idx) >> 0).toByte).put((pix(idx) >> 24).toByte)
        }
      }
    }
    buf.flip
    texID = GL11.glGenTextures()
    bind()
    GL12.glTexImage3D(GL30.GL_TEXTURE_2D_ARRAY, 0, GL11.GL_RGBA, texSize, texSize, 
        width / texSize * height / texSize, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf)
    if (GL.getCapabilities().GL_EXT_texture_filter_anisotropic) {
      val amt = Math.min(16f, GL11.glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT));
      // This does not work for the triangular images!
//    GL11.glTexParameterf(GL30.GL_TEXTURE_2D_ARRAY, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, amt);
    } else {
      System.out.println("Anisotropic filtering is not supported :(");
    }
    GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST_MIPMAP_LINEAR)
    GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
    GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE)
    GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE)
    GL30.glGenerateMipmap(GL30.GL_TEXTURE_2D_ARRAY)
  }// TODO: Mipmaping produces jagged edges on triangular images
  
  protected def reload(): Unit = {
    unload()
    load()
  }

  def bind(): Unit = {
    if (TextureArray.boundTextureArray != this) {
      TextureArray.boundTextureArray = this
      GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, texID)
    }
  }

  def unload(): Unit = {
    if (TextureArray.boundTextureArray == this) TextureArray.unbind();
    GL11.glDeleteTextures(texID)
  }
}
