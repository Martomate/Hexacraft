package hexacraft.text.font

import hexacraft.text.font.FntFile.{CharacterPadding, CharLine}

import scala.collection.mutable

class FontMetaData(private val characters: Map[Int, Character], val lineHeight: Double) {
  def spaceWidth: Double = characters.get(' '.toInt).map(_.screenBounds.xAdvance).getOrElse(0)

  def getCharacter(ascii: Int): Character = characters.getOrElse(ascii, null)

  def atLineHeight(lineHeight: Double): FontMetaData = {
    val scaledChars = characters.view.mapValues(_.withScaledScreenBounds(lineHeight / this.lineHeight)).toMap
    new FontMetaData(scaledChars, lineHeight)
  }
}

object FontMetaData {
  private val DesiredPadding: Int = 3

  extension (ch: CharLine) {

    /** Converts the character from 'pixel-space' to 'screen-space' (with a line height of 1) */
    def toCharacter(
        desiredPadding: Int,
        padding: CharacterPadding,
        lineHeightInImage: Int,
        imageSize: Int
    ): Character = {
      val extraLeftPadding = padding.left - desiredPadding
      val extraTopPadding = padding.top - desiredPadding

      val pixelsPerLine: Double = (lineHeightInImage - padding.vertical).toDouble

      val adjustedWidth: Int = ch.width - (padding.horizontal - 2 * desiredPadding)
      val adjustedHeight: Int = ch.height - (padding.vertical - 2 * desiredPadding)

      Character(
        ch.id,
        Character.TextureBounds(
          (ch.x + extraLeftPadding).toDouble / imageSize,
          (ch.y + extraTopPadding).toDouble / imageSize,
          adjustedWidth.toDouble / imageSize,
          adjustedHeight.toDouble / imageSize
        ),
        Character.ScreenBounds(
          (ch.xOffset + extraLeftPadding) / pixelsPerLine,
          (ch.yOffset + extraTopPadding) / pixelsPerLine,
          adjustedWidth / pixelsPerLine,
          adjustedHeight / pixelsPerLine,
          (ch.xAdvance - padding.horizontal) / pixelsPerLine
        )
      )
    }
  }

  def fromFntFile(contents: FntFile): FontMetaData = {
    val desiredPadding = FontMetaData.DesiredPadding
    val padding = contents.info.padding
    val fontLineHeight = contents.common.lineHeight
    val imageSize = contents.common.scaleW

    val characters: mutable.Map[Int, Character] = mutable.HashMap.empty

    for ch <- contents.chars do {
      val c = ch.toCharacter(desiredPadding, padding, fontLineHeight, imageSize)
      if c != null then {
        characters.put(c.id, c)
      }
    }

    new FontMetaData(characters.toMap, lineHeight = 1)
  }
}
