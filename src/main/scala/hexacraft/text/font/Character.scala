package hexacraft.text.font

/** A glyph in the font texture atlas.
  *
  * @param id
  *   the ASCII value of the character.
  * @param textureBounds
  *   the coordinates and size of this character in the texture atlas.
  * @param screenBounds
  *   the screen space information for the character when the font size is 1.
  */
case class Character(id: Int, textureBounds: Character.TextureBounds, screenBounds: Character.ScreenBounds) {
  def withScaledScreenBounds(scale: Double): Character =
    this.copy(screenBounds = screenBounds.scaled(scale))
}

object Character {
  case class TextureBounds(x: Double, y: Double, w: Double, h: Double)

  /** @param x
    * the x distance from the cursor to the left edge of the character's quad in screen space.
    * @param y
    * the y distance from the cursor to the top edge of the character's quad in screen space.
    * @param w
    * the width of the character's quad in screen space.
    * @param h
    * the height of the character's quad in screen space.
    * @param xAdvance
    * how far the cursor should advance in screen space after adding this character.
    */
  case class ScreenBounds(x: Double, y: Double, w: Double, h: Double, xAdvance: Double) {
    def scaled(scale: Double): ScreenBounds =
      ScreenBounds(x * scale, y * scale, w * scale, h * scale, xAdvance * scale)
  }
}
