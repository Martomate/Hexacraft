package hexacraft.text.font

import hexacraft.text.font.FntFile.*

import scala.collection.mutable

case class FntFile(
    info: InfoLine,
    common: CommonLine,
    chars: Seq[CharLine]
)

object FntFile {
  case class CharacterPadding(top: Int, left: Int, bottom: Int, right: Int) {
    def vertical: Int = top + bottom
    def horizontal: Int = left + right
  }

  case class InfoLine(padding: CharacterPadding)

  case class CommonLine(lineHeight: Int, scaleW: Int)

  case class CharLine(id: Int, x: Int, y: Int, width: Int, height: Int, xOffset: Int, yOffset: Int, xAdvance: Int)

  private class LineParser(values: Map[String, String]) {
    def info: InfoLine = {
      val Seq(t, l, b, r) = values("padding").split(",").toSeq.map(_.toInt)
      val padding = CharacterPadding(t, l, b, r)
      InfoLine(padding)
    }

    def common: CommonLine = {
      CommonLine(lineHeight = values("lineHeight").toInt, scaleW = values("scaleW").toInt)
    }

    def char: CharLine = {
      CharLine(
        id = values("id").toInt,
        x = values("x").toInt,
        y = values("y").toInt,
        width = values("width").toInt,
        height = values("height").toInt,
        xOffset = values("xoffset").toInt,
        yOffset = values("yoffset").toInt,
        xAdvance = values("xadvance").toInt
      )
    }
  }

  def fromLines(lines: Seq[String]): FntFile = {
    var infoLine: InfoLine = null
    var commonLine: CommonLine = null
    val charLines = mutable.ArrayBuffer.empty[CharLine]

    for line <- lines do {
      line.split(' ').toSeq match {
        case lineType +: props =>
          val values: mutable.Map[String, String] = mutable.HashMap.empty

          for part <- props do {
            val valuePairs: Array[String] = part.split("=")
            if (valuePairs.length == 2) values.put(valuePairs(0), valuePairs(1))
          }

          val parser = LineParser(values.toMap)

          lineType match {
            case "info"   => infoLine = parser.info
            case "common" => commonLine = parser.common
            case "char"   => charLines += parser.char
            case _        =>
          }
        case _ =>
      }
    }

    FntFile(infoLine, commonLine, charLines.toSeq)
  }
}
