package hexacraft.text

import hexacraft.infra.fs.Bundle
import hexacraft.renderer.TextureSingle
import hexacraft.text.font.{FntFile, Font, FontMetaData}

import scala.collection.mutable

object Fonts {
  private val fonts = mutable.HashMap.empty[String, Font]

  loadFont("Verdana", "font/Verdana")

  def loadFont(name: String, path: String): Font = {
    if fonts contains name then {
      return fonts(name)
    }

    val atlas = TextureSingle.getTexture(path).id

    val metaDataLines = Bundle.locate(s"$path.fnt").get.readLines()
    val metaData = FontMetaData.fromFntFile(FntFile.fromLines(metaDataLines))

    val f = Font(atlas, metaData)
    fonts(name) = f
    f
  }

  def get(name: String): Option[Font] = fonts.get(name)
}
