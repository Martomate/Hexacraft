package com.martomate.hexacraft.menu.main

import java.io.File

import com.martomate.hexacraft.game.GameScene
import com.martomate.hexacraft.gui.comp._
import com.martomate.hexacraft.gui.location.LocationInfo16x9
import com.martomate.hexacraft.scene.{GameWindowExtended, MenuScene}
import com.martomate.hexacraft.world.settings.WorldSettings

import scala.util.{Random, Try}

class NewWorldMenu(implicit window: GameWindowExtended) extends MenuScene{
  addComponent(new Label("World name", LocationInfo16x9(0.3f, 0.7f + 0.075f, 0.2f, 0.05f), 3f, false).withColor(1, 1, 1))
  private val nameTF = new TextField(LocationInfo16x9(0.3f, 0.7f, 0.4f, 0.075f), maxFontSize = 2.5f)
  addComponent(nameTF)

  addComponent(new Label("World size", LocationInfo16x9(0.3f, 0.55f + 0.075f, 0.2f, 0.05f), 3f, false).withColor(1, 1, 1))
  private val sizeTF = new TextField(LocationInfo16x9(0.3f, 0.55f, 0.4f, 0.075f), maxFontSize = 2.5f)
  addComponent(sizeTF)

  addComponent(new Label("World seed", LocationInfo16x9(0.3f, 0.4f + 0.075f, 0.2f, 0.05f), 3f, false).withColor(1, 1, 1))
  private val seedTF = new TextField(LocationInfo16x9(0.3f, 0.4f, 0.4f, 0.075f), maxFontSize = 2.5f)
  addComponent(seedTF)

  addComponent(Button("Cancel", LocationInfo16x9(0.3f, 0.05f, 0.19f, 0.1f)){ window.scenes.popScene() })
  addComponent(Button("Create world", LocationInfo16x9(0.51f, 0.05f, 0.19f, 0.1f))(createWorld()))

  private def createWorld(): Unit = {
    try {
      val (_, file) = {
        val filteredName = nameTF.text.map(c => if (c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == ' ') c else '_')
        val nameBase = if (filteredName.trim.nonEmpty) filteredName else "New World"
        val savesFolder = new File(window.saveFolder, "saves")

        var name: String = null
        var file: File = null
        var count = 0
        do {
          count += 1
          name = if (count == 1) nameBase else nameBase + " " + count
          file = new File(savesFolder, name)
        } while (file.exists())

        (name, file)
      }
      val size = Try(sizeTF.text.toByte).toOption.filter(s => s >= 0 && s <= 20)
      val seed = Some(seedTF.text).filter(_.nonEmpty).map(s => new Random(s.##.toLong << 32 | s.reverse.##).nextLong())
      window.scenes.popScenesUntil(MenuScene.isMainMenu)
      window.scenes.pushScene(new GameScene(file, WorldSettings(Some(nameTF.text), size, seed)))
    } catch {
      case _: Exception =>
      // TODO: complain about the input
    }
  }
}
