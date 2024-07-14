package hexacraft.renderer

import hexacraft.infra.fs.Bundle
import hexacraft.infra.gpu.OpenGL
import hexacraft.infra.gpu.OpenGL.*
import hexacraft.util.Resource

import org.lwjgl.BufferUtils

import java.awt.image.BufferedImage
import java.nio.ByteBuffer

object TextureSingle {
  private val textures = collection.mutable.Map.empty[String, TextureSingle]

  private var boundTexture: TextureSingle = null.asInstanceOf[TextureSingle]

  def unbind(): Unit = {
    TextureSingle.boundTexture = null
    OpenGL.glBindTexture(TextureTarget.Texture2D, TextureId.none)
  }

  def getTexture(name: String): TextureSingle = {
    textures.get(name) match {
      case Some(tex) => tex
      case None =>
        val tex = loadTexture(name)
        TextureSingle.textures += name -> tex
        tex
    }
  }

  private def loadTexture(name: String): TextureSingle = {
    val image = Bundle.locate(s"$name.png").get.readImage()

    val width = image.getWidth
    val height = image.getHeight
    val buf = readRgbaPixels(image)

    val texID = OpenGL.glGenTextures()
    val tex = new TextureSingle(texID, width, height)

    tex.bind()

    tex.setPixels(buf)
    tex.setFiltering(TexMinFilter.Linear, TexMagFilter.Nearest)
    tex.setWrapping(TexWrap.ClampToEdge, TexWrap.ClampToEdge)

    tex
  }

  private def readRgbaPixels(image: BufferedImage): ByteBuffer = {
    val pix = image.getRGB(0, 0, image.getWidth, image.getHeight, null, 0, image.getWidth)
    val buf = BufferUtils.createByteBuffer(pix.length * 4)

    for i <- 0 until pix.size do {
      buf.put((pix(i) >> 16).toByte)
      buf.put((pix(i) >> 8).toByte)
      buf.put((pix(i) >> 0).toByte)
      buf.put((pix(i) >> 24).toByte)
    }

    buf.flip
    buf
  }
}

class TextureSingle(val id: TextureId, val width: Int, val height: Int) extends Resource {

  /** @note the texture must be bound */
  def setPixels(rgbaPixels: ByteBuffer): Unit = {
    require(rgbaPixels.remaining() == 4 * width * height, "wrong number of pixels")

    OpenGL.glTexImage2D(
      TextureTarget.Texture2D,
      0,
      TextureInternalFormat.Rgba,
      width,
      height,
      0,
      TexelDataFormat.Rgba,
      TexelDataType.UnsignedByte,
      rgbaPixels
    )
  }

  /** @note the texture must be bound */
  def setFiltering(min: TexMinFilter, mag: TexMagFilter): Unit = {
    OpenGL.glTexParameteri(TextureTarget.Texture2D, TexIntParameter.MinFilter(min))
    OpenGL.glTexParameteri(TextureTarget.Texture2D, TexIntParameter.MagFilter(mag))
  }

  /** @note the texture must be bound */
  def setWrapping(s: TexWrap, t: TexWrap): Unit = {
    OpenGL.glTexParameteri(TextureTarget.Texture2D, TexIntParameter.TextureWrapS(s))
    OpenGL.glTexParameteri(TextureTarget.Texture2D, TexIntParameter.TextureWrapT(t))
  }

  def bind(): Unit = {
    if TextureSingle.boundTexture != this then {
      TextureSingle.boundTexture = this
      OpenGL.glBindTexture(TextureTarget.Texture2D, id)
    }
  }

  def unload(): Unit = {
    if TextureSingle.boundTexture == this then {
      TextureSingle.unbind()
    }
    OpenGL.glDeleteTextures(id)
  }
}

object TextureArray {
  private val textures = collection.mutable.Map.empty[String, TextureArray]

  private var boundTextureArray: TextureArray = null.asInstanceOf[TextureArray]

  def unbind(): Unit = {
    TextureArray.boundTextureArray = null
    OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2DArray, OpenGL.TextureId.none)
  }

  def getTextureArray(name: String): TextureArray = textures(name)

  def registerTextureArray(name: String, texSize: Int, images: Seq[PixelArray]): TextureArray = {
    if !textures.contains(name) then {
      val textureArray = loadTextureArray(texSize, images)
      textures += name -> textureArray
      textureArray
    } else {
      textures(name)
    }
  }

  private def loadTextureArray(texSize: Int, images: Seq[PixelArray]): TextureArray = {
    val width = texSize * images.length
    val height = texSize

    val numTextures = width / texSize * height / texSize
    val numBytes = height * width * 4

    val buf = BufferUtils.createByteBuffer(numBytes)
    for image <- images do {
      putRgbaPixels(image.pixels, texSize, buf)
    }
    buf.flip

    val texID = OpenGL.glGenTextures()
    val tex = new TextureArray(texID, texSize, images)

    tex.bind()

    tex.setPixels(buf, numTextures)

    tex.setFiltering(TexMinFilter.NearestMipmapLinear, TexMagFilter.Nearest)
    tex.setWrapping(TexWrap.ClampToEdge, TexWrap.ClampToEdge)
    tex.generateMipmaps() // TODO: generate mipmaps manually and take image.isTriImage into account

    tex
  }

  private def putRgbaPixels(argbIntPixels: Array[Int], texSize: Int, dest: ByteBuffer): Unit = {
    val pix = argbIntPixels

    for j <- 0 until texSize do {
      for i <- 0 until texSize do {
        val idx = i + j * texSize
        dest.put((pix(idx) >> 16).toByte)
        dest.put((pix(idx) >> 8).toByte)
        dest.put((pix(idx) >> 0).toByte)
        dest.put((pix(idx) >> 24).toByte)
      }
    }
  }
}

class TextureArray private (texID: OpenGL.TextureId, val texSize: Int, images: Seq[PixelArray]) extends Resource {

  /** @note the texture must be bound */
  def setPixels(rgbaPixels: ByteBuffer, numTextures: Int): Unit = {
    require(rgbaPixels.remaining() == numTextures * 4 * texSize * texSize, "wrong number of pixels")

    OpenGL.glTexImage3D(
      OpenGL.TextureTarget.Texture2DArray,
      0,
      TextureInternalFormat.Rgba,
      texSize,
      texSize,
      numTextures,
      0,
      TexelDataFormat.Rgba,
      TexelDataType.UnsignedByte,
      rgbaPixels
    )
  }

  /** @note the texture must be bound */
  def setFiltering(min: TexMinFilter, mag: TexMagFilter): Unit = {
    OpenGL.glTexParameteri(TextureTarget.Texture2DArray, TexIntParameter.MinFilter(min))
    OpenGL.glTexParameteri(TextureTarget.Texture2DArray, TexIntParameter.MagFilter(mag))
  }

  /** @note the texture must be bound */
  def setWrapping(s: TexWrap, t: TexWrap): Unit = {
    OpenGL.glTexParameteri(TextureTarget.Texture2DArray, TexIntParameter.TextureWrapS(s))
    OpenGL.glTexParameteri(TextureTarget.Texture2DArray, TexIntParameter.TextureWrapT(t))
  }

  /** @note the texture must be bound */
  def generateMipmaps(): Unit = {
    OpenGL.glGenerateMipmap(TextureTarget.Texture2DArray)
  }

  def bind(): Unit = {
    if TextureArray.boundTextureArray != this then {
      TextureArray.boundTextureArray = this
      OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2DArray, texID)
    }
  }

  def unload(): Unit = {
    if TextureArray.boundTextureArray == this then {
      TextureArray.unbind()
    }
    OpenGL.glDeleteTextures(texID)
  }
}
