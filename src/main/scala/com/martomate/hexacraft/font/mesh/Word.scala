package com.martomate.hexacraft.font.mesh

import scala.collection.mutable

class Word {
  private val characters: mutable.ArrayBuffer[Character] = new mutable.ArrayBuffer[Character]
  private var width: Double = 0

  def addCharacter(character: Character): Unit =
    characters += character
    width += character.screenBounds.xAdvance

  def getCharacters: Seq[Character] = characters.toSeq

  def getWordWidth: Double = width
}
