package hexacraft.text.layout

import hexacraft.text.font.Character
import hexacraft.text.font.Character.{ScreenBounds, TextureBounds}
import hexacraft.text.font.FontMetaData

import munit.FunSuite

class LineBreakerTest extends FunSuite {
  def linesToText(lines: Seq[Line], characters: Map[Int, Character]): Seq[String] =
    lines.map(
      _.words
        .map(_.getCharacters.map(ch => characters.find(_._2.id == ch.id).get._1.toChar).mkString)
        .mkString(" ")
    )

  test("wrap lines") {
    val chA = Character(0, TextureBounds(0, 0, 0, 0), ScreenBounds(0, 0, 1, 2, 1))
    val chB = Character(1, TextureBounds(0, 0, 0, 0), ScreenBounds(0, 0, 1, 2, 1))
    val chSpace = Character(2, TextureBounds(0, 0, 0, 0), ScreenBounds(0, 0, 1, 2, 1))

    val characters = Map('a'.toInt -> chA, 'b'.toInt -> chB, ' '.toInt -> chSpace)
    val font = FontMetaData(characters, 1)

    assertEquals(
      linesToText(LineBreaker(8.5).layout("aa aa aa", font), characters),
      Seq("aa aa aa")
    )
    assertEquals(
      linesToText(LineBreaker(7.5).layout("aa aa aa", font), characters),
      Seq("aa aa", "aa")
    )
    assertEquals(
      linesToText(LineBreaker(5.5).layout("aa aa aa", font), characters),
      Seq("aa aa", "aa")
    )
    assertEquals(
      linesToText(LineBreaker(4.5).layout("aa aa aa", font), characters),
      Seq("aa", "aa", "aa")
    )
    assertEquals(
      linesToText(LineBreaker(2.5).layout("abba", font), characters),
      Seq("ab", "ba")
    )
    assertEquals(
      linesToText(LineBreaker(2.5).layout("abbaabba", font), characters),
      Seq("ab", "ba", "ab", "ba")
    )
    assertEquals(
      linesToText(LineBreaker(4.5).layout("ababa bb aa", font), characters),
      Seq("abab", "a bb", "aa")
    )
  }
}
