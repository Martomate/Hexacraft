package com.martomate.hexacraft.text.mesh

import com.martomate.hexacraft.text.font.FontMetaData.toCharacter
import com.martomate.hexacraft.text.font.{FntFile, FontMetaData}
import com.martomate.hexacraft.text.font.FntFile.{CharacterPadding, CharLine, CommonLine, InfoLine}

import munit.FunSuite

class FontMetaDataTest extends FunSuite {
  private val delta = 1e-6

  private val desiredPadding = 3

  test("fromFileContents can extract space width") {
    val contents = FntFile(
      InfoLine(padding = CharacterPadding(10, 11, 12, 13)),
      CommonLine(lineHeight = 95, scaleW = 512),
      Seq(CharLine(32, 1, 2, 3, 4, 5, 6, xAdvance = 31))
    )

    val metaFile = FontMetaData.fromFntFile(contents)

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

    val contents = FntFile(InfoLine(padding), CommonLine(lineHeight, scaleW), Seq(line))
    val metaFile = FontMetaData.fromFntFile(contents)

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
}
