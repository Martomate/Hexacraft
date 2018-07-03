package hexacraft.gui.menu.main

import java.io.File
import java.lang.String

import hexacraft.Main
import hexacraft.gui.comp._
import hexacraft.gui.menu.MenuScene
import hexacraft.scene.GameScene
import hexacraft.world.WorldSettings

import scala.util.{Random, Try}
import scala.util.hashing.Hashing

class NewWorldMenu extends MenuScene{
  // TODO: add text fields and other settings
  addComponent(new Label("World name", LocationInfo(0.3f, 0.7f + 0.075f, 0.2f, 0.05f), 3f, false).withColor(1, 1, 1))
  private val nameTF = new TextField(LocationInfo(0.3f, 0.7f, 0.4f, 0.075f), maxFontSize = 1.5f)
  addComponent(nameTF)
  addComponent(new Label("World size", LocationInfo(0.3f, 0.55f + 0.075f, 0.2f, 0.05f), 3f, false).withColor(1, 1, 1))
  private val sizeTF = new TextField(LocationInfo(0.3f, 0.55f, 0.4f, 0.075f), maxFontSize = 1.5f)
  addComponent(sizeTF)
  addComponent(new Label("World seed", LocationInfo(0.3f, 0.4f + 0.075f, 0.2f, 0.05f), 3f, false).withColor(1, 1, 1))
  private val seedTF = new TextField(LocationInfo(0.3f, 0.4f, 0.4f, 0.075f), maxFontSize = 1.5f)
  addComponent(seedTF)

  addComponent(new Button("Cancel", LocationInfo(0.3f, 0.1f, 0.19f, 0.1f))({ Main.popScene() }))
  addComponent(new Button("Create world", LocationInfo(0.51f, 0.1f, 0.19f, 0.1f))({
    try {
      val (_, file) = {
        val filteredName = nameTF.text.map(c => if (c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == ' ') c else '_')
        val nameBase = if (filteredName.trim.nonEmpty) filteredName else "New World"
        val savesFolder = new File(Main.saveFolder, "saves")

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
      Main.popScenesUntilMainMenu()
      Main.pushScene(new GameScene(file, WorldSettings(Some(nameTF.text), size, seed)))
    } catch {
      case _: Exception =>
        // TODO: complain about the input
    }
  }))

}
