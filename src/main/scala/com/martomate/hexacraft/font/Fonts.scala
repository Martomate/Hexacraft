package com.martomate.hexacraft.font

import com.martomate.hexacraft.font.mesh.{FontType, FontMetaData}
import com.martomate.hexacraft.infra.fs.FileUtils
import com.martomate.hexacraft.renderer.TextureSingle

import scala.collection.mutable

object Fonts {
  private val fonts = mutable.HashMap.empty[String, FontType]

  loadFont("Verdana", "font/Verdana")

  def loadFont(name: String, path: String): FontType = {
    if (fonts contains name) fonts(name)
    else {
      val atlas = TextureSingle.getTexture(path).id

      val metaDataFile = FileUtils.getResourceFile(path + ".fnt").get
      val metaDataLines = FileUtils.readLinesFromUrl(metaDataFile)
      val metaData = FontMetaData.fromLines(metaDataLines)

      val f = FontType.fromAtlas(atlas, metaData)
      fonts(name) = f
      f
    }
  }

  def get(name: String): Option[FontType] = fonts.get(name)
}
