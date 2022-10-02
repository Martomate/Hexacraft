package com.martomate.hexacraft.menu.main

import java.io.File
import com.martomate.hexacraft.game.GameScene
import com.martomate.hexacraft.gui.{GameWindowExtended, LocationInfo16x9}
import com.martomate.hexacraft.gui.comp._
import com.martomate.hexacraft.menu.MenuScene
import com.martomate.hexacraft.world.settings.{WorldProviderFromFile, WorldSettings}

import scala.util.Random

class NewWorldMenu(implicit window: GameWindowExtended) extends MenuScene {
  addComponent(
    new Label("World name", LocationInfo16x9(0.3f, 0.7f + 0.075f, 0.2f, 0.05f), 3f, false)
      .withColor(1, 1, 1)
  )
  private val nameTF = new TextField(LocationInfo16x9(0.3f, 0.7f, 0.4f, 0.075f), maxFontSize = 2.5f)
  addComponent(nameTF)

  addComponent(
    new Label("World size", LocationInfo16x9(0.3f, 0.55f + 0.075f, 0.2f, 0.05f), 3f, false)
      .withColor(1, 1, 1)
  )
  private val sizeTF =
    new TextField(LocationInfo16x9(0.3f, 0.55f, 0.4f, 0.075f), maxFontSize = 2.5f)
  addComponent(sizeTF)

  addComponent(
    new Label("World seed", LocationInfo16x9(0.3f, 0.4f + 0.075f, 0.2f, 0.05f), 3f, false)
      .withColor(1, 1, 1)
  )
  private val seedTF = new TextField(LocationInfo16x9(0.3f, 0.4f, 0.4f, 0.075f), maxFontSize = 2.5f)
  addComponent(seedTF)

  addComponent(Button("Cancel", LocationInfo16x9(0.3f, 0.05f, 0.19f, 0.1f)) {
    window.scenes.popScene()
  })
  addComponent(Button("Create world", LocationInfo16x9(0.51f, 0.05f, 0.19f, 0.1f))(createWorld()))

  private def createWorld(): Unit = {
    try {
      val baseFolder = new File(window.saveFolder, "saves")
      val file = uniqueFile(baseFolder, cleanupFileName(nameTF.text))
      val size = sizeTF.text.toByteOption.filter(s => s >= 0 && s <= 20)
      val seed = Some(seedTF.text)
        .filter(_.nonEmpty)
        .map(s => s.toLongOption.getOrElse(new Random(s.##.toLong << 32 | s.reverse.##).nextLong()))
      window.scenes.popScenesUntil(MenuScene.isMainMenu)
      window.scenes.pushScene(
        new GameScene(new WorldProviderFromFile(file, WorldSettings(Some(nameTF.text), size, seed)))
      )
    } catch {
      case _: Exception =>
      // TODO: complain about the input
    }
  }

  private def uniqueFile(baseFolder: File, fileName: String): File = {
    var file: File = null
    var count = 0
    while
      count += 1
      val name = if (count == 1) fileName else fileName + " " + count
      file = new File(baseFolder, name)
      file.exists()
    do ()

    file
  }

  private def cleanupFileName(fileName: String): String = {
    def charValid(c: Char): Boolean =
      c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == ' '
    val name = fileName.map(c => if (charValid(c)) c else '_').trim
    if (name.nonEmpty) name else "New World"
  }
}
