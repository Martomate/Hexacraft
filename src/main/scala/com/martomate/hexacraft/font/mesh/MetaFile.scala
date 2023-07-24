package com.martomate.hexacraft.font.mesh

import scala.collection.mutable

class MetaFile(private val metaData: Map[Int, Character]) {
  def getSpaceWidth: Double = metaData.get(TextMeshCreator.SPACE_ASCII).map(_.xAdvance).getOrElse(0)

  def getCharacter(ascii: Int): Character = metaData.getOrElse(ascii, null)
}

object MetaFile {
  private val PAD_TOP: Int = 0
  private val PAD_LEFT: Int = 1
  private val PAD_BOTTOM: Int = 2
  private val PAD_RIGHT: Int = 3
  private val DESIRED_PADDING: Int = 3
  private val SPLITTER: String = " "
  private val NUMBER_SEPARATOR: String = ","

  def fromLines(lines: Seq[String]): MetaFile = MetaFile.fromFileContents(MetaFileContents.fromLines(lines))

  case class MetaFileContents(
      info: MetaFileInfoLine,
      common: MetaFileCommonLine,
      charLines: Seq[MetaFileCharLine]
  )

  object MetaFileContents {
    def fromLines(lines: Seq[String]): MetaFileContents =
      var infoLine: MetaFileInfoLine = null
      var commonLine: MetaFileCommonLine = null
      val charLines = mutable.ArrayBuffer.empty[MetaFileCharLine]

      for line <- lines do
        line.split(MetaFile.SPLITTER).toSeq match
          case lineType +: props =>
            val values: mutable.Map[String, String] = mutable.HashMap.empty

            for part <- props do
              val valuePairs: Array[String] = part.split("=")
              if (valuePairs.length == 2) values.put(valuePairs(0), valuePairs(1))

            lineType match
              case "info"   => infoLine = MetaFileInfoLine.fromStrings(values.toMap)
              case "common" => commonLine = MetaFileCommonLine.fromStrings(values.toMap)
              case "char"   => charLines += MetaFileCharLine.fromStrings(values.toMap)
              case _        =>
          case _ =>

      MetaFileContents(
        info = infoLine,
        common = commonLine,
        charLines = charLines.toSeq
      )
  }

  case class CharacterPadding(top: Int, left: Int, bottom: Int, right: Int) {
    def vertical: Int = top + bottom
    def horizontal: Int = left + right
  }

  case class MetaFileInfoLine(padding: CharacterPadding)

  object MetaFileInfoLine {
    def fromStrings(values: Map[String, String]): MetaFileInfoLine = {
      val ints = values("padding").split(MetaFile.NUMBER_SEPARATOR).toSeq.map(_.toInt)
      MetaFileInfoLine(
        padding = CharacterPadding(
          ints(MetaFile.PAD_TOP),
          ints(MetaFile.PAD_LEFT),
          ints(MetaFile.PAD_BOTTOM),
          ints(MetaFile.PAD_RIGHT)
        )
      )
    }
  }

  case class MetaFileCommonLine(lineHeight: Int, scaleW: Int)

  object MetaFileCommonLine {
    def fromStrings(values: Map[String, String]): MetaFileCommonLine =
      MetaFileCommonLine(
        lineHeight = values("lineHeight").toInt,
        scaleW = values("scaleW").toInt
      )
  }

  case class MetaFileCharLine(
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
        xTextureCoord = (this.x.toDouble + extraLeftPadding) / imageSize,
        yTextureCoord = (this.y.toDouble + extraTopPadding) / imageSize,
        xTexSize = width.toDouble / imageSize,
        yTexSize = height.toDouble / imageSize,
        xOffset = (this.xOffset + extraLeftPadding) * horizontalPerPixelSize,
        yOffset = (this.yOffset + extraTopPadding) * verticalPerPixelSize,
        sizeX = width * horizontalPerPixelSize,
        sizeY = height * verticalPerPixelSize,
        xAdvance = (this.xAdvance - padding.horizontal) * horizontalPerPixelSize
      )
  }

  object MetaFileCharLine {
    def fromStrings(values: Map[String, String]): MetaFileCharLine =
      MetaFileCharLine(
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

  def fromFileContents(contents: MetaFileContents): MetaFile =
    val desiredPadding = MetaFile.DESIRED_PADDING
    val desiredLineHeight = TextMeshCreator.LINE_HEIGHT
    val padding = contents.info.padding
    val fontLineHeight = contents.common.lineHeight
    val imageSize = contents.common.scaleW

    val metaData: mutable.Map[Int, Character] = mutable.HashMap.empty

    for ch <- contents.charLines do
      val c = ch.toCharacter(desiredPadding, desiredLineHeight, padding, fontLineHeight, imageSize)
      if c != null then metaData.put(c.id, c)

    new MetaFile(metaData.toMap)
}
