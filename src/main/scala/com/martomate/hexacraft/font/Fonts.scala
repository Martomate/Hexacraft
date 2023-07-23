package com.martomate.hexacraft.font

import com.martomate.hexacraft.font.mesh.FontType
import com.martomate.hexacraft.infra.fs.FileUtils
import com.martomate.hexacraft.renderer.TextureSingle

import scala.collection.mutable

object Fonts {
  private val fonts = mutable.HashMap.empty[String, FontType]

  loadFont("Verdana", "font/Verdana")

  def loadFont(name: String, path: String): FontType = {
    if (fonts contains name) fonts(name)
    else {
      val f = FontType.fromUrl(
        TextureSingle.getTexture(path).id,
        FileUtils.getResourceFile(path + ".fnt").get
      )
      fonts(name) = f
      f
    }
  }

  def get(name: String): Option[FontType] = fonts.get(name)
}
