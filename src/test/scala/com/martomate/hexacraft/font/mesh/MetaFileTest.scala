package com.martomate.hexacraft.font.mesh

import java.nio.file.Files
import munit.FunSuite

class MetaFileTest extends FunSuite {
  private val delta = 1e-9

  private val desiredPadding = 3
  private val desiredLineHeight = 0.03

  private val basicFontFileContents =
    """
      |info face="SomeFont" size=72 bold=0 italic=0 charset="" unicode=0 stretchH=100 smooth=1 aa=1 padding=4,14,24,34 spacing=0,0
      |common lineHeight=95 base=73 scaleW=512 scaleH=512 pages=1 packed=0
      |page id=0 file="some_font.png"
      |chars count=2
      |char id=32      x=0    y=0    width=0    height=0    xoffset=-3   yoffset=69   xadvance=31   page=0    chnl=0
      |char id=67       x=439  y=145  width=60   height=70   xoffset=6    yoffset=15   xadvance=78   page=0    chnl=0
      |""".stripMargin.trim

  test("fromUrl reads meta file from disk") {
    val metaFile =
      val file = Files.createTempFile("meta", ".fnt")
      Files.write(file, basicFontFileContents.getBytes)

      try MetaFile.fromUrl(file.toUri.toURL)
      finally Files.delete(file)

    val extraLeftPadding = 14 - desiredPadding
    val extraTopPadding = 4 - desiredPadding

    val paddingWidth = 14 + 34
    val paddingHeight = 4 + 24

    val width = 60 - (paddingWidth - 2 * desiredPadding)
    val height = 70 - (paddingHeight - 2 * desiredPadding)

    val pixelScale = desiredLineHeight / (95 - paddingHeight)

    val ch = metaFile.getCharacter(67)

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

    assertEqualsDouble(metaFile.getSpaceWidth, (31 - paddingWidth) * pixelScale, delta)
  }
}
