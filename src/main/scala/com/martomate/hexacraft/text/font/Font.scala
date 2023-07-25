package com.martomate.hexacraft.text.font

import com.martomate.hexacraft.infra.gpu.OpenGL

import scala.collection.mutable

class Font(val textureAtlas: OpenGL.TextureId, baseMetaData: FontMetaData) {
  private val metaDataCache = mutable.Map.empty[Double, FontMetaData]

  def getMetaData(lineHeight: Double): FontMetaData =
    metaDataCache.getOrElseUpdate(lineHeight, baseMetaData.atLineHeight(lineHeight))
}
