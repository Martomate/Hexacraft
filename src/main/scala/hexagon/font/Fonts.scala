package hexagon.font

import java.io.File

import fontMeshCreator.FontType
import hexagon.resource.TextureSingle

import scala.collection.mutable

object Fonts {
  private val fonts = mutable.HashMap.empty[String, FontType]

  loadFont("Verdana", "font/Verdana")

  def loadFont(name: String, path: String): FontType = {
    if (fonts contains name) fonts(name)
    else {
      val f = new FontType(TextureSingle.getTexture(path).id, new File("res/" + path + ".fnt"))
      fonts(name) = f
      f
    }
  }

  def get(name: String): Option[FontType] = fonts.get(name)
}
