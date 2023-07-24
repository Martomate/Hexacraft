package com.martomate.hexacraft.font.mesh

import com.martomate.hexacraft.font.mesh.MetaFile.{
  CharacterPadding,
  MetaFileCharLine,
  MetaFileCommonLine,
  MetaFileContents,
  MetaFileInfoLine
}

import munit.FunSuite

class MetaFileTest extends FunSuite {
  private val delta = 1e-6

  private val desiredPadding = 3
  private val desiredLineHeight = 0.03

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
      MetaFileContents.fromLines(basicFontMetaFileContents.split('\n').toSeq),
      MetaFileContents(
        MetaFileInfoLine(padding = CharacterPadding(top = 4, left = 14, bottom = 24, right = 34)),
        MetaFileCommonLine(lineHeight = 95, scaleW = 512),
        Seq(
          MetaFileCharLine(id = 32, x = 0, y = 0, width = 0, height = 0, xOffset = -3, yOffset = 69, xAdvance = 31),
          MetaFileCharLine(id = 67, x = 439, y = 145, width = 60, height = 70, xOffset = 6, yOffset = 15, xAdvance = 78)
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

    val info = MetaFileInfoLine(padding = CharacterPadding(4, 14, 24, 34))
    val common = MetaFileCommonLine(lineHeight = 95, scaleW = 512)
    val chars = Seq(
      MetaFileCharLine(id = 67, x = 439, y = 145, width = 60, height = 70, xOffset = 6, yOffset = 15, xAdvance = 78)
    )

    assertEquals(MetaFileContents.fromLines(lines), MetaFileContents(info, common, chars))
  }

  test("fromFileContents can extract space width") {
    val contents = MetaFileContents(
      MetaFileInfoLine(padding = CharacterPadding(10, 11, 12, 13)),
      MetaFileCommonLine(lineHeight = 95, scaleW = 512),
      Seq(MetaFileCharLine(32, 1, 2, 3, 4, 5, 6, xAdvance = 31))
    )

    val metaFile = MetaFile.fromFileContents(contents)

    val paddingWidth = 11 + 13
    val paddingHeight = 10 + 12
    val pixelScale = desiredLineHeight / (95 - paddingHeight)

    assertEqualsDouble(metaFile.getSpaceWidth, (31 - paddingWidth) * pixelScale, delta)
  }

  test("fromFileContents can extract characters") {
    val padding = CharacterPadding(10, 11, 12, 13)
    val lineHeight = 95
    val scaleW = 512
    val line =
      MetaFileCharLine(id = 67, x = 439, y = 145, width = 60, height = 70, xOffset = 6, yOffset = 15, xAdvance = 78)

    val contents = MetaFileContents(MetaFileInfoLine(padding), MetaFileCommonLine(lineHeight, scaleW), Seq(line))
    val metaFile = MetaFile.fromFileContents(contents)

    assertEquals(
      metaFile.getCharacter(67),
      line.toCharacter(desiredPadding, desiredLineHeight, padding, lineHeight, scaleW)
    )
  }

  test("characters can be transformed from pixel space to screen space") {
    val padding = CharacterPadding(10, 11, 12, 13)
    val ch =
      MetaFileCharLine(id = 67, x = 439, y = 145, width = 60, height = 70, xOffset = 6, yOffset = 15, xAdvance = 78)
        .toCharacter(desiredPadding, desiredLineHeight, padding, 95, 512)

    val extraLeftPadding = 11 - desiredPadding
    val extraTopPadding = 10 - desiredPadding

    val paddingWidth = 11 + 13
    val paddingHeight = 10 + 12

    val width = 60 - (paddingWidth - 2 * desiredPadding)
    val height = 70 - (paddingHeight - 2 * desiredPadding)

    val pixelScale = desiredLineHeight / (95 - paddingHeight).toDouble

    assertEquals(ch.id, 67)
    assertEqualsDouble(ch.xTextureCoord, (439 + extraLeftPadding) / 512d, delta)
    assertEqualsDouble(ch.yTextureCoord, (145 + extraTopPadding) / 512d, delta)
    assertEqualsDouble(ch.xTexSize, width / 512d, delta)
    assertEqualsDouble(ch.yTexSize, height / 512d, delta)
    assertEqualsDouble(ch.xOffset, (6 + extraLeftPadding) * pixelScale, delta)
    assertEqualsDouble(ch.yOffset, (15 + extraTopPadding) * pixelScale, delta)
    assertEqualsDouble(ch.sizeX, width * pixelScale, delta)
    assertEqualsDouble(ch.sizeY, height * pixelScale, delta)
    assertEqualsDouble(ch.xAdvance, (78 - paddingWidth) * pixelScale, delta)
  }

  test("fromLines parses and converts file contents into a MetaFile") {
    val lines = basicFontMetaFileContents.split('\n').toSeq

    val metaFile = MetaFile.fromLines(lines)
    val expectedMetaFile = MetaFile.fromFileContents(MetaFileContents.fromLines(lines))

    assertEquals(metaFile.getSpaceWidth, expectedMetaFile.getSpaceWidth)
    assertEquals(metaFile.getCharacter(32), expectedMetaFile.getCharacter(32))
    assertEquals(metaFile.getCharacter(67), expectedMetaFile.getCharacter(67))
    assertEquals(metaFile.getCharacter(68), null)
  }
}
