package com.martomate.hexacraft.font.mesh

import scala.collection.mutable

class FontMetaData(private val characters: Map[Int, Character], val lineHeight: Double) {
  def spaceWidth: Double = characters.get(' '.toInt).map(_.screenBounds.xAdvance).getOrElse(0)

  def getCharacter(ascii: Int): Character = characters.getOrElse(ascii, null)

  def atLineHeight(lineHeight: Double): FontMetaData =
    val scaledChars = characters.view.mapValues(_.withScaledScreenBounds(lineHeight / this.lineHeight)).toMap
    new FontMetaData(scaledChars, lineHeight)
}

object FontMetaData {
  private val DesiredPadding: Int = 3

  def fromLines(lines: Seq[String]): FontMetaData = FontMetaData.fromFileContents(FileContents.fromLines(lines))

  case class FileContents(
      info: InfoLine,
      common: CommonLine,
      charLines: Seq[CharLine]
  )

  object FileContents {
    def fromLines(lines: Seq[String]): FileContents =
      var infoLine: InfoLine = null
      var commonLine: CommonLine = null
      val charLines = mutable.ArrayBuffer.empty[CharLine]

      for line <- lines do
        line.split(" ").toSeq match
          case lineType +: props =>
            val values: mutable.Map[String, String] = mutable.HashMap.empty

            for part <- props do
              val valuePairs: Array[String] = part.split("=")
              if (valuePairs.length == 2) values.put(valuePairs(0), valuePairs(1))

            lineType match
              case "info"   => infoLine = InfoLine.fromStrings(values.toMap)
              case "common" => commonLine = CommonLine.fromStrings(values.toMap)
              case "char"   => charLines += CharLine.fromStrings(values.toMap)
              case _        =>
          case _ =>

      FileContents(infoLine, commonLine, charLines.toSeq)
  }

  case class CharacterPadding(top: Int, left: Int, bottom: Int, right: Int) {
    def vertical: Int = top + bottom
    def horizontal: Int = left + right
  }

  case class InfoLine(padding: CharacterPadding)

  object InfoLine {
    def fromStrings(values: Map[String, String]): InfoLine = {
      val ints = values("padding").split(",").toSeq.map(_.toInt)
      val padding = CharacterPadding(
        ints(0),
        ints(1),
        ints(2),
        ints(3)
      )
      InfoLine(padding)
    }
  }

  case class CommonLine(lineHeight: Int, scaleW: Int)

  object CommonLine {
    def fromStrings(values: Map[String, String]): CommonLine =
      CommonLine(
        lineHeight = values("lineHeight").toInt,
        scaleW = values("scaleW").toInt
      )
  }

  case class CharLine(id: Int, x: Int, y: Int, width: Int, height: Int, xOffset: Int, yOffset: Int, xAdvance: Int) {

    /** Converts the character from 'pixel-space' to 'screen-space' (with a line height of 1) */
    def toCharacter(desiredPadding: Int, padding: CharacterPadding, lineHeightInImage: Int, imageSize: Int): Character =
      val extraLeftPadding = padding.left - desiredPadding
      val extraTopPadding = padding.top - desiredPadding

      val pixelsPerLine: Double = (lineHeightInImage - padding.vertical).toDouble

      val adjustedWidth: Int = width - (padding.horizontal - 2 * desiredPadding)
      val adjustedHeight: Int = height - (padding.vertical - 2 * desiredPadding)

      Character(
        id,
        Character.TextureBounds(
          (x + extraLeftPadding).toDouble / imageSize,
          (y + extraTopPadding).toDouble / imageSize,
          adjustedWidth.toDouble / imageSize,
          adjustedHeight.toDouble / imageSize
        ),
        Character.ScreenBounds(
          (xOffset + extraLeftPadding) / pixelsPerLine,
          (yOffset + extraTopPadding) / pixelsPerLine,
          adjustedWidth / pixelsPerLine,
          adjustedHeight / pixelsPerLine,
          (xAdvance - padding.horizontal) / pixelsPerLine
        )
      )
  }

  object CharLine {
    def fromStrings(values: Map[String, String]): CharLine =
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

  def fromFileContents(contents: FileContents): FontMetaData =
    val desiredPadding = FontMetaData.DesiredPadding
    val padding = contents.info.padding
    val fontLineHeight = contents.common.lineHeight
    val imageSize = contents.common.scaleW

    val characters: mutable.Map[Int, Character] = mutable.HashMap.empty

    for ch <- contents.charLines do
      val c = ch.toCharacter(desiredPadding, padding, fontLineHeight, imageSize)
      if c != null then characters.put(c.id, c)

    new FontMetaData(characters.toMap, lineHeight = 1)
}
