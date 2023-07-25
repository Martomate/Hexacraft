package com.martomate.hexacraft.font.mesh

import com.martomate.hexacraft.font.mesh.FontMetaData.{CharacterPadding, CharLine, CommonLine, FileContents, InfoLine}

import munit.FunSuite

class FontMetaDataTest extends FunSuite {
  private val delta = 1e-6

  private val desiredPadding = 3

  private val basicFontMetaFileContents =
    """
      |info face="SomeFont" size=72 bold=0 italic=0 charset="" unicode=0 stretchH=100 smooth=1 aa=1 padding=4,14,24,34 spacing=0,0
      |common lineHeight=95 base=73 scaleW=512 scaleH=512 pages=1 packed=0
      |page id=0 file="some_font.png"
      |chars count=2
      |char id=32      x=0    y=0    width=0    height=0    xoffset=-3   yoffset=69   xadvance=31   page=0    chnl=0
      |char id=67       x=439  y=145  width=60   height=70   xoffset=6    yoffset=15   xadvance=78   page=0    chnl=0
      |""".stripMargin.trim

  test("basic file can be parsed") {
    assertEquals(
      FileContents.fromLines(basicFontMetaFileContents.split('\n').toSeq),
      FileContents(
        InfoLine(padding = CharacterPadding(top = 4, left = 14, bottom = 24, right = 34)),
        CommonLine(lineHeight = 95, scaleW = 512),
        Seq(
          CharLine(id = 32, x = 0, y = 0, width = 0, height = 0, xOffset = -3, yOffset = 69, xAdvance = 31),
          CharLine(id = 67, x = 439, y = 145, width = 60, height = 70, xOffset = 6, yOffset = 15, xAdvance = 78)
        )
      )
    )
  }

  test("only the needed fields are parsed") {
    val lines = Seq(
      "info    padding=4,14,24,34",
      "common  lineHeight=95  scaleW=512",
      "char    id=67  x=439  y=145  width=60  height=70  xoffset=6   yoffset=15  xadvance=78"
    )

    val info = InfoLine(padding = CharacterPadding(4, 14, 24, 34))
    val common = CommonLine(lineHeight = 95, scaleW = 512)
    val chars = Seq(
      CharLine(id = 67, x = 439, y = 145, width = 60, height = 70, xOffset = 6, yOffset = 15, xAdvance = 78)
    )

    assertEquals(FileContents.fromLines(lines), FileContents(info, common, chars))
  }

  test("fromFileContents can extract space width") {
    val contents = FileContents(
      InfoLine(padding = CharacterPadding(10, 11, 12, 13)),
      CommonLine(lineHeight = 95, scaleW = 512),
      Seq(CharLine(32, 1, 2, 3, 4, 5, 6, xAdvance = 31))
    )

    val metaFile = FontMetaData.fromFileContents(contents)

    val paddingWidth = 11 + 13
    val paddingHeight = 10 + 12
    val pixelScale = 1.0 / (95 - paddingHeight)

    assertEqualsDouble(metaFile.spaceWidth, (31 - paddingWidth) * pixelScale, delta)
  }

  test("fromFileContents can extract characters") {
    val padding = CharacterPadding(10, 11, 12, 13)
    val lineHeight = 95
    val scaleW = 512
    val line =
      CharLine(id = 67, x = 439, y = 145, width = 60, height = 70, xOffset = 6, yOffset = 15, xAdvance = 78)

    val contents = FileContents(InfoLine(padding), CommonLine(lineHeight, scaleW), Seq(line))
    val metaFile = FontMetaData.fromFileContents(contents)

    assertEquals(
      metaFile.getCharacter(67),
      line.toCharacter(desiredPadding, padding, lineHeight, scaleW)
    )
  }

  test("characters can be transformed from pixel space to screen space") {
    val padding = CharacterPadding(10, 11, 12, 13)
    val ch =
      CharLine(id = 67, x = 439, y = 145, width = 60, height = 70, xOffset = 6, yOffset = 15, xAdvance = 78)
        .toCharacter(desiredPadding, padding, 95, 512)

    val extraLeftPadding = 11 - desiredPadding
    val extraTopPadding = 10 - desiredPadding

    val paddingWidth = 11 + 13
    val paddingHeight = 10 + 12

    val width = 60 - (paddingWidth - 2 * desiredPadding)
    val height = 70 - (paddingHeight - 2 * desiredPadding)

    val pixelScale = 1.0 / (95 - paddingHeight)

    assertEquals(ch.id, 67)
    assertEqualsDouble(ch.textureBounds.x, (439 + extraLeftPadding) / 512d, delta)
    assertEqualsDouble(ch.textureBounds.y, (145 + extraTopPadding) / 512d, delta)
    assertEqualsDouble(ch.textureBounds.w, width / 512d, delta)
    assertEqualsDouble(ch.textureBounds.h, height / 512d, delta)
    assertEqualsDouble(ch.screenBounds.x, (6 + extraLeftPadding) * pixelScale, delta)
    assertEqualsDouble(ch.screenBounds.y, (15 + extraTopPadding) * pixelScale, delta)
    assertEqualsDouble(ch.screenBounds.w, width * pixelScale, delta)
    assertEqualsDouble(ch.screenBounds.h, height * pixelScale, delta)
    assertEqualsDouble(ch.screenBounds.xAdvance, (78 - paddingWidth) * pixelScale, delta)
  }

  test("fromLines parses and converts file contents into a MetaFile") {
    val lines = basicFontMetaFileContents.split('\n').toSeq

    val metaFile = FontMetaData.fromLines(lines)
    val expectedMetaFile = FontMetaData.fromFileContents(FileContents.fromLines(lines))

    assertEquals(metaFile.spaceWidth, expectedMetaFile.spaceWidth)
    assertEquals(metaFile.getCharacter(32), expectedMetaFile.getCharacter(32))
    assertEquals(metaFile.getCharacter(67), expectedMetaFile.getCharacter(67))
    assertEquals(metaFile.getCharacter(68), null)
  }
}
