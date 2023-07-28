package hexacraft.renderer

import hexacraft.infra.gpu.OpenGL
import hexacraft.infra.gpu.OpenGL.{TexelDataFormat, TexelDataType, TextureInternalFormat}
import hexacraft.util.{Resource, ResourceWrapper}
import org.lwjgl.BufferUtils

object TextureArray {
  private val textures = collection.mutable.Map.empty[String, TextureArray]

  private var boundTextureArray: TextureArray = _

  def unbind(): Unit = {
    TextureArray.boundTextureArray = null
    OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2DArray, OpenGL.TextureId.none)
  }

  def getTextureArray(name: String): TextureArray = textures(name)

  def registerTextureArray(
      name: String,
      texSize: Int,
      images: ResourceWrapper[Seq[TextureToLoad]]
  ): TextureArray = {
    if (!textures.contains(name)) new TextureArray(name, texSize, images)
    else textures(name)
  }
}

class TextureArray(
    val name: String,
    val texSize: Int,
    wrappedImages: ResourceWrapper[Seq[TextureToLoad]]
) extends Resource
    with Texture {
  private var texID: OpenGL.TextureId = _

  TextureArray.textures += name -> this

  load()

  protected def load(): Unit = {
    val images = wrappedImages.get
    val height = texSize
    val width = texSize * images.length
    val buf = BufferUtils.createByteBuffer(height * width * 4)
    for (image <- images) {
      val pix = image.pixels
      for (j <- 0 until texSize) {
        for (i <- 0 until texSize) {
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

  protected def reload(): Unit = {
    unload()
    load()
  }

  def bind(): Unit = {
    if (TextureArray.boundTextureArray != this) {
      TextureArray.boundTextureArray = this
      OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2DArray, texID)
    }
  }

  def unload(): Unit = {
    if (TextureArray.boundTextureArray == this) TextureArray.unbind()
    OpenGL.glDeleteTextures(texID)
  }
}
