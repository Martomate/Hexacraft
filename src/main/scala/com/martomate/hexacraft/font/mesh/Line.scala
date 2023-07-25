package com.martomate.hexacraft.font.mesh

import scala.collection.mutable

class Line(spaceWidth: Double, val maxWidth: Double) {
  private val _words: mutable.ArrayBuffer[Word] = new mutable.ArrayBuffer[Word]
  private var _width: Double = 0

  def width: Double = _width

  def words: Seq[Word] = _words.toSeq

  def attemptToAddWord(word: Word): Boolean =
    var additionalWidth: Double = word.getWordWidth
    if _words.nonEmpty then additionalWidth += spaceWidth

    if _width + additionalWidth <= maxWidth then
      _words += word
      _width += additionalWidth
      true
    else false
}
