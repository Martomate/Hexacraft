package com.martomate.hexacraft.font.mesh

/**
 * Simple data structure class holding information about a certain glyph in the
 * font texture atlas. All sizes are for a font-size of 1.
 *
 * @author Karl
 *
 * @param id
 *            - the ASCII value of the character.
 * @param xTextureCoord
 *            - the x texture coordinate for the top left corner of the
 *              character in the texture atlas.
 * @param yTextureCoord
 *            - the y texture coordinate for the top left corner of the
 *              character in the texture atlas.
 * @param xTexSize
 *            - the width of the character in the texture atlas.
 * @param yTexSize
 *            - the height of the character in the texture atlas.
 * @param xOffset
 *            - the x distance from the curser to the left edge of the
 *              character's quad.
 * @param yOffset
 *            - the y distance from the curser to the top edge of the
 *              character's quad.
 * @param sizeX
 *            - the width of the character's quad in screen space.
 * @param sizeY
 *            - the height of the character's quad in screen space.
 * @param xAdvance
 *            - how far in pixels the cursor should advance after adding
 *              this character.
 */
class Character (var id: Int, var xTextureCoord: Double, var yTextureCoord: Double, val xTexSize: Double, val yTexSize: Double, var xOffset: Double, var yOffset: Double, var sizeX: Double, var sizeY: Double, var xAdvance: Double) {
  private var xMaxTextureCoord = xTexSize + xTextureCoord
  private var yMaxTextureCoord = yTexSize + yTextureCoord

  def getId = id

  def getxTextureCoord = xTextureCoord

  def getyTextureCoord = yTextureCoord

  def getXMaxTextureCoord = xMaxTextureCoord

  def getYMaxTextureCoord = yMaxTextureCoord

  def getxOffset = xOffset

  def getyOffset = yOffset

  def getSizeX = sizeX

  def getSizeY = sizeY

  def getxAdvance = xAdvance
}
