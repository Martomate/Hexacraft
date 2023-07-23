package com.martomate.hexacraft.font.mesh

import com.martomate.hexacraft.infra.fs.FileUtils

import java.io.{BufferedReader, IOException}
import java.net.URL
import java.util.stream.Collectors
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

  def fromUrl(file: URL): MetaFile =
    MetaFile.fromFileContents(MetaFileContents.fromLines(readMetaFile(file)))

  private def readMetaFile(file: URL) =
    val reader = FileUtils.getBufferedReader(file)

    val lines = reader
      .lines()
      .collect(Collectors.toList)
      .toArray(n => new Array[String](n))
      .toSeq

    try reader.close()
    catch
      case e: IOException =>
        e.printStackTrace()

    lines

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

  case class MetaFileInfoLine(padding: Seq[Int])

  object MetaFileInfoLine {
    def fromStrings(values: Map[String, String]): MetaFileInfoLine =
      MetaFileInfoLine(
        padding = values("padding").split(MetaFile.NUMBER_SEPARATOR).toSeq.map(_.toInt)
      )
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
  )

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

  def fromFileContents(contents: MetaFileContents): MetaFile = {
    val padding: Seq[Int] = contents.info.padding
    val paddingWidth: Int = padding(MetaFile.PAD_LEFT) + padding(MetaFile.PAD_RIGHT)
    val paddingHeight: Int = padding(MetaFile.PAD_TOP) + padding(MetaFile.PAD_BOTTOM)

    val leftPadding = padding(MetaFile.PAD_LEFT)
    val topPadding = padding(MetaFile.PAD_TOP)
    val extraLeftPadding = leftPadding - MetaFile.DESIRED_PADDING
    val extraTopPadding = topPadding - MetaFile.DESIRED_PADDING

    val lineHeightPixels: Int = contents.common.lineHeight - paddingHeight
    val verticalPerPixelSize: Double = TextMeshCreator.LINE_HEIGHT / lineHeightPixels.toDouble
    val horizontalPerPixelSize: Double = verticalPerPixelSize

    val imageSize = contents.common.scaleW

    /** Loads all the data about one character in the texture atlas and converts it all from 'pixels'
      * to 'screen-space' before storing. The effects of padding are also removed from the data.
      */
    def loadCharacter(ch: MetaFileCharLine): Character =
      val width: Int = ch.width - (paddingWidth - 2 * MetaFile.DESIRED_PADDING)
      val height: Int = ch.height - (paddingHeight - 2 * MetaFile.DESIRED_PADDING)

      Character(
        ch.id,
        xTextureCoord = (ch.x.toDouble + extraLeftPadding) / imageSize,
        yTextureCoord = (ch.y.toDouble + extraTopPadding) / imageSize,
        xTexSize = width.toDouble / imageSize,
        yTexSize = height.toDouble / imageSize,
        xOffset = (ch.xOffset + extraLeftPadding) * horizontalPerPixelSize,
        yOffset = (ch.yOffset + extraTopPadding) * verticalPerPixelSize,
        sizeX = width * horizontalPerPixelSize,
        sizeY = height * verticalPerPixelSize,
        xAdvance = (ch.xAdvance - paddingWidth) * horizontalPerPixelSize
      )

    val metaData: mutable.Map[Int, Character] = mutable.HashMap.empty

    for ch <- contents.charLines do
      val c: Character = loadCharacter(ch)
      if (c != null) metaData.put(c.id, c)

    new MetaFile(metaData.toMap)
  }
}
