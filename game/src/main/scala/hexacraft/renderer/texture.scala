package hexacraft.renderer

import hexacraft.infra.fs.FileUtils
import hexacraft.infra.gpu.OpenGL
import hexacraft.infra.gpu.OpenGL.*
import hexacraft.util.Resource

import org.lwjgl.BufferUtils

import javax.imageio.ImageIO

object TextureSingle {
  private val textures = collection.mutable.Map.empty[String, TextureSingle]

  private var boundTexture: TextureSingle = _

  def unbind(): Unit = {
    TextureSingle.boundTexture = null
    OpenGL.glBindTexture(TextureTarget.Texture2D, TextureId.none)
  }

  def getTexture(name: String): TextureSingle = {
    textures.getOrElse(name, new TextureSingle(name))
  }
}

class TextureSingle(val name: String) extends Resource {
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

    for i <- 0 until pix.size do {
      buf.put((pix(i) >> 16).toByte)
      buf.put((pix(i) >> 8).toByte)
      buf.put((pix(i) >> 0).toByte)
      buf.put((pix(i) >> 24).toByte)
    }

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

  def bind(): Unit = {
    if TextureSingle.boundTexture != this then {
      TextureSingle.boundTexture = this
      OpenGL.glBindTexture(TextureTarget.Texture2D, texID)
    }
  }

  def unload(): Unit = {
    if TextureSingle.boundTexture == this then {
      TextureSingle.unbind()
    }
    OpenGL.glDeleteTextures(texID)
  }
}

object TextureArray {
  private val textures = collection.mutable.Map.empty[String, TextureArray]

  private var boundTextureArray: TextureArray = _

  def unbind(): Unit = {
    TextureArray.boundTextureArray = null
    OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2DArray, OpenGL.TextureId.none)
  }

  def getTextureArray(name: String): TextureArray = textures(name)

  def registerTextureArray(name: String, texSize: Int, images: Seq[PixelArray]): TextureArray = {
    if !textures.contains(name) then {
      new TextureArray(name, texSize, images)
    } else {
      textures(name)
    }
  }
}

class TextureArray(
    val name: String,
    val texSize: Int,
    images: Seq[PixelArray]
) extends Resource {
  private var texID: OpenGL.TextureId = _

  TextureArray.textures += name -> this

  load()

  protected def load(): Unit = {
    val height = texSize
    val width = texSize * images.length
    val buf = BufferUtils.createByteBuffer(height * width * 4)
    for image <- images do {
      val pix = image.pixels
      for j <- 0 until texSize do {
        for i <- 0 until texSize do {
          val idx = i + j * texSize
          buf.put((pix(idx) >> 16).toByte)
          buf.put((pix(idx) >> 8).toByte)
          buf.put((pix(idx) >> 0).toByte)
          buf.put((pix(idx) >> 24).toByte)
        }
      }
    }
    buf.flip
    texID = OpenGL.glGenTextures()
    bind()
    OpenGL.glTexImage3D(
      OpenGL.TextureTarget.Texture2DArray,
      0,
      TextureInternalFormat.Rgba,
      texSize,
      texSize,
      width / texSize * height / texSize,
      0,
      TexelDataFormat.Rgba,
      TexelDataType.UnsignedByte,
      buf
    )

    // TODO: generate mipmaps manually and take image.isTriImage into account

    OpenGL.glTexParameteri(
      OpenGL.TextureTarget.Texture2DArray,
      OpenGL.TexIntParameter.MinFilter(OpenGL.TexMinFilter.NearestMipmapLinear)
    )
    OpenGL.glTexParameteri(
      OpenGL.TextureTarget.Texture2DArray,
      OpenGL.TexIntParameter.MagFilter(OpenGL.TexMagFilter.Nearest)
    )
    OpenGL.glTexParameteri(
      OpenGL.TextureTarget.Texture2DArray,
      OpenGL.TexIntParameter.TextureWrapS(OpenGL.TexWrap.ClampToEdge)
    )
    OpenGL.glTexParameteri(
      OpenGL.TextureTarget.Texture2DArray,
      OpenGL.TexIntParameter.TextureWrapT(OpenGL.TexWrap.ClampToEdge)
    )
    OpenGL.glGenerateMipmap(OpenGL.TextureTarget.Texture2DArray)
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
