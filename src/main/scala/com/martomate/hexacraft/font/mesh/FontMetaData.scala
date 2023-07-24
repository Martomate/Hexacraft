package com.martomate.hexacraft.font.mesh

import scala.collection.mutable

class FontMetaData(private val metaData: Map[Int, Character]) {
  def getSpaceWidth: Double = metaData.get(' '.toInt).map(_.xAdvance).getOrElse(0)

  def getCharacter(ascii: Int): Character = metaData.getOrElse(ascii, null)
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

  case class CharLine(
      id: Int,
      x: Int,
      y: Int,
      width: Int,
      height: Int,
      xOffset: Int,
      yOffset: Int,
      xAdvance: Int
  ) {

    /** Converts the character from 'pixel-space' to 'screen-space */
    def toCharacter(
        desiredPadding: Int,
        desiredLineHeight: Double,
        padding: CharacterPadding,
        fontLineHeight: Int,
        imageSize: Int
    ): Character =
      val extraLeftPadding = padding.left - desiredPadding
      val extraTopPadding = padding.top - desiredPadding

      val lineHeightPixels: Int = fontLineHeight - padding.vertical
      val verticalPerPixelSize: Double = desiredLineHeight / lineHeightPixels.toDouble
      val horizontalPerPixelSize: Double = verticalPerPixelSize

      val width: Int = this.width - (padding.horizontal - 2 * desiredPadding)
      val height: Int = this.height - (padding.vertical - 2 * desiredPadding)

      Character(
        this.id,
        Character.TextureBounds(
          (this.x.toDouble + extraLeftPadding) / imageSize,
          (this.y.toDouble + extraTopPadding) / imageSize,
          width.toDouble / imageSize,
          height.toDouble / imageSize
        ),
        xOffset = (this.xOffset + extraLeftPadding) * horizontalPerPixelSize,
        yOffset = (this.yOffset + extraTopPadding) * verticalPerPixelSize,
        sizeX = width * horizontalPerPixelSize,
        sizeY = height * verticalPerPixelSize,
        xAdvance = (this.xAdvance - padding.horizontal) * horizontalPerPixelSize
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
    val desiredLineHeight = TextMeshBuilder.BaseLineHeight
    val padding = contents.info.padding
    val fontLineHeight = contents.common.lineHeight
    val imageSize = contents.common.scaleW

    val metaData: mutable.Map[Int, Character] = mutable.HashMap.empty

    for ch <- contents.charLines do
      val c = ch.toCharacter(desiredPadding, desiredLineHeight, padding, fontLineHeight, imageSize)
      if c != null then metaData.put(c.id, c)

    new FontMetaData(metaData.toMap)
}
