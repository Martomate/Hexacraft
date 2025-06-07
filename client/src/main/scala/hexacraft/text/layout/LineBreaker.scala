package hexacraft.text.layout

import hexacraft.text.font.{Character, FontMetaData}

import scala.collection.mutable

class LineBreaker(maxLineWidth: Float) {

  /** Splits the text into lines no wider than the given max line width */
  def layout(text: String, font: FontMetaData): Seq[Line] = {
    val lines = new mutable.ArrayBuffer[Line]

    var currentLine: Line = Line(font.spaceWidth, maxLineWidth)

    val textWithoutTrailingSpaces = text.stripTrailing()

    val words: Array[Word] =
      for s <- textWithoutTrailingSpaces.split(' ') yield {
        val w = new Word
        for c <- s.toCharArray do {
          val character: Character = font.getCharacter(c.toInt)
          w.addCharacter(character)
        }
        w
      }
    val numTrailingSpaces = text.length - textWithoutTrailingSpaces.length

    for w <- words ++ Seq.fill(numTrailingSpaces)(new Word()) do {
      val added: Boolean = currentLine.attemptToAddWord(w)
      if !added then {
        val couldFit = if currentLine.words.nonEmpty then {
          lines += currentLine
          currentLine = Line(font.spaceWidth, maxLineWidth)
          currentLine.attemptToAddWord(w)
        } else false

        // The following is a workaround for auto-resizing texts to detect an overflow
        if !couldFit then {
          var w2 = Word()
          for c <- w.getCharacters do {
            if w2.getWordWidth + c.screenBounds.xAdvance <= maxLineWidth then {
              w2.addCharacter(c)
            } else {
              currentLine.attemptToAddWord(w2)
              lines += currentLine
              currentLine = Line(font.spaceWidth, maxLineWidth)
              w2 = Word()
              w2.addCharacter(c)
            }
          }
          if w2.getCharacters.nonEmpty then {
            currentLine.attemptToAddWord(w2)
            lines += currentLine
            currentLine = Line(font.spaceWidth, maxLineWidth)
          }
        }
      }
    }

    if currentLine.words.nonEmpty then {
      lines += currentLine
    }

    lines.toSeq
  }
}
