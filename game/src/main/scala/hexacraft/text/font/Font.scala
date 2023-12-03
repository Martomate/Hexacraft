package hexacraft.text.font

import hexacraft.infra.gpu.OpenGL

import scala.collection.mutable

class Font(val textureAtlas: OpenGL.TextureId, baseMetaData: FontMetaData) {
  private val metaDataCache = mutable.Map.empty[Double, FontMetaData]

  def getMetaData(lineHeight: Double): FontMetaData =
    metaDataCache.getOrElseUpdate(lineHeight, baseMetaData.atLineHeight(lineHeight))
}
