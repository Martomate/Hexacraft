package com.martomate.hexacraft.text.font

import com.martomate.hexacraft.text.font.FntFile.{CharacterPadding, CharLine, CommonLine, InfoLine}

import munit.FunSuite

class FntFileTest extends FunSuite {
  private val delta = 1e-6

  test("basic file can be parsed") {
    assertEquals(
      FntFile.fromLines("""
          |info face="SomeFont" size=72 bold=0 italic=0 charset="" unicode=0 stretchH=100 smooth=1 aa=1 padding=4,14,24,34 spacing=0,0
          |common lineHeight=95 base=73 scaleW=512 scaleH=512 pages=1 packed=0
          |page id=0 file="some_font.png"
          |chars count=2
          |char id=32      x=0    y=0    width=0    height=0    xoffset=-3   yoffset=69   xadvance=31   page=0    chnl=0
          |char id=67       x=439  y=145  width=60   height=70   xoffset=6    yoffset=15   xadvance=78   page=0    chnl=0
          |""".stripMargin.trim.split('\n').toSeq),
      FntFile(
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

    assertEquals(FntFile.fromLines(lines), FntFile(info, common, chars))
  }
}
