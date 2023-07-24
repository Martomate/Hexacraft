package com.martomate.hexacraft.font.mesh

import scala.collection.mutable

class WordWrapper(metaData: FontMetaData, fontSize: Float, maxLineLength: Float) {

  /** Splits the text into lines no longer than the given max line length */
  def wrap(text: String): Seq[Line] =
    val lines = new mutable.ArrayBuffer[Line]

    var currentLine: Line = new Line(metaData.getSpaceWidth, fontSize, maxLineLength)

    val words: Array[Word] =
      for s <- text.split(' ')
      yield
        val w = new Word(fontSize)
        for c <- s.toCharArray do
          val character: Character = metaData.getCharacter(c.toInt)
          w.addCharacter(character)
        w

    for w <- words do
      val added: Boolean = currentLine.attemptToAddWord(w)
      if !added then
        lines += currentLine
        currentLine = new Line(metaData.getSpaceWidth, fontSize, maxLineLength)
        val couldFit = currentLine.attemptToAddWord(w)

        // The following is a workaround for auto-resizing texts to detect an overflow
        if !couldFit then lines += new Line(metaData.getSpaceWidth, fontSize, maxLineLength)

    if currentLine.getWords.nonEmpty then lines += currentLine

    lines.toSeq

}
