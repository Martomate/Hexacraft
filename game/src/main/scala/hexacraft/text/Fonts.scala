package hexacraft.text

import hexacraft.infra.fs.FileUtils
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

    val metaDataFile = FileUtils.getResourceFile(path + ".fnt").get
    val metaDataLines = FileUtils.readLinesFromUrl(metaDataFile)
    val metaData = FontMetaData.fromFntFile(FntFile.fromLines(metaDataLines))

    val f = Font(atlas, metaData)
    fonts(name) = f
    f
  }

  def get(name: String): Option[Font] = fonts.get(name)
}
