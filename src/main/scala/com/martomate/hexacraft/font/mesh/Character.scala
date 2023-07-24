package com.martomate.hexacraft.font.mesh

/** Simple data structure class holding information about a certain glyph in the font texture atlas.
  * All sizes are for a font-size of 1.
  *
  * @param id
  *   the ASCII value of the character.
  * @param textureBounds
  *   the coordinates and size of this character in the texture atlas.
  * @param xOffset
  *   the x distance from the cursor to the left edge of the character's quad in screen space.
  * @param yOffset
  *   the y distance from the cursor to the top edge of the character's quad in screen space.
  * @param sizeX
  *   the width of the character's quad in screen space.
  * @param sizeY
  *   the height of the character's quad in screen space.
  * @param xAdvance
  *   how far the cursor should advance in screen space after adding this character.
  */
case class Character(
    id: Int,
    textureBounds: Character.TextureBounds,
    xOffset: Double,
    yOffset: Double,
    sizeX: Double,
    sizeY: Double,
    xAdvance: Double
)

object Character {
  case class TextureBounds(x: Double, y: Double, w: Double, h: Double)
}
