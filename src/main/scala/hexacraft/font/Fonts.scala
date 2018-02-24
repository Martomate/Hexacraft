package hexacraft.font

import fontMeshCreator.FontType
import hexacraft.resource.TextureSingle
import hexacraft.util.FileUtils

import scala.collection.mutable

object Fonts {
  private val fonts = mutable.HashMap.empty[String, FontType]

  loadFont("Verdana", "font/Verdana")

  def loadFont(name: String, path: String): FontType = {
    if (fonts contains name) fonts(name)
    else {
      val f = new FontType(TextureSingle.getTexture(path).id, FileUtils.getResourceFile(path + ".fnt").get)
      fonts(name) = f
      f
    }
  }

  def get(name: String): Option[FontType] = fonts.get(name)
}