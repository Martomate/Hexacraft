package com.martomate.hexacraft.font.mesh

import scala.collection.mutable

/** Represents a line of text during the loading of a text.
  *
  * @author
  *   Karl
  *
  * Creates an empty line.
  * @param spaceWidth
  *   the screen-space width of a space character.
  * @param fontSize
  *   the size of font being used.
  * @param maxLength
  *   the screen-space maximum length of a line.
  */
class Line(val spaceWidth: Double, val fontSize: Double, val maxLength: Double) {
  private val spaceSize: Double = spaceWidth * fontSize
  private val words: mutable.ArrayBuffer[Word] = new mutable.ArrayBuffer[Word]
  private var _currentLineLength: Double = 0

  /** Attempt to add a word to the line. If the line can fit the word in without reaching the
    * maximum line length then the word is added and the line length increased.
    *
    * @param word
    *   the word to try to add.
    * @return `true` if the word has successfully been added to the line.
    */
  def attemptToAddWord(word: Word): Boolean = {
    var additionalLength: Double = word.getWordWidth
    additionalLength += (if (words.nonEmpty) spaceSize else 0)
    if (_currentLineLength + additionalLength <= maxLength) {
      words += word
      _currentLineLength += additionalLength
      true
    } else false
  }

  /** @return
    *   The current screen-space length of the line.
    */
  def currentLineLength: Double = _currentLineLength

  /** @return
    *   The list of words in the line.
    */
  def getWords: Seq[Word] = words.toSeq
}
