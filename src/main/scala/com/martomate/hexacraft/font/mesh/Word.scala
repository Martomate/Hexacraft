package com.martomate.hexacraft.font.mesh

import scala.collection.mutable

/** Create a new empty word.
  *
  * @param fontSize
  *   the font size of the text which this word is in.
  */
class Word(val fontSize: Double) {
  private val characters: mutable.ArrayBuffer[Character] = new mutable.ArrayBuffer[Character]
  private var width: Double = 0

  /** Adds a character to the end of the current word and increases the screen-space width of the
    * word.
    *
    * @param character
    *   the character to be added.
    */
  def addCharacter(character: Character): Unit = {
    characters += character
    width += character.xAdvance * fontSize
  }

  /** @return
    *   The list of characters in the word.
    */
  def getCharacters: Seq[Character] = characters.toSeq

  /** @return
    *   The width of the word in terms of screen size.
    */
  def getWordWidth: Double = width
}
