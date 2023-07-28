package hexacraft.text.layout

import hexacraft.text.font.{Character, FontMetaData}

import scala.collection.mutable

class LineBreaker(maxLineWidth: Float) {

  /** Splits the text into lines no wider than the given max line width */
  def layout(text: String, font: FontMetaData): Seq[Line] =
    val lines = new mutable.ArrayBuffer[Line]

    var currentLine: Line = Line(font.spaceWidth, maxLineWidth)

    val words: Array[Word] =
      for s <- text.split(' ')
      yield
        val w = new Word
        for c <- s.toCharArray do
          val character: Character = font.getCharacter(c.toInt)
          w.addCharacter(character)
        w

    for w <- words do
      val added: Boolean = currentLine.attemptToAddWord(w)
      if !added then
        lines += currentLine
        currentLine = Line(font.spaceWidth, maxLineWidth)
        val couldFit = currentLine.attemptToAddWord(w)

        // The following is a workaround for auto-resizing texts to detect an overflow
        if !couldFit then lines += Line(font.spaceWidth, maxLineWidth)

    if currentLine.words.nonEmpty then lines += currentLine

    lines.toSeq

}
