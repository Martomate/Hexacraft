package hexacraft.menu

import hexacraft.GameMouse
import hexacraft.gui.{LocationInfo, MenuScene}
import hexacraft.gui.comp.{Button, Label, TextField}
import hexacraft.menu.NewWorldMenu.Event
import hexacraft.world.settings.WorldSettings

import java.io.File
import scala.util.Random

object NewWorldMenu {
  enum Event:
    case StartGame(saveDir: File, settings: WorldSettings)
    case GoBack
}

class NewWorldMenu(saveFolder: File)(onEvent: NewWorldMenu.Event => Unit)(using GameMouse) extends MenuScene {
  addComponent(
    new Label("World name", LocationInfo.from16x9(0.3f, 0.7f + 0.075f, 0.2f, 0.05f), 3f, false)
      .withColor(1, 1, 1)
  )
  private val nameTF = new TextField(LocationInfo.from16x9(0.3f, 0.7f, 0.4f, 0.075f), maxFontSize = 2.5f)
  addComponent(nameTF)

  addComponent(
    new Label("World size", LocationInfo.from16x9(0.3f, 0.55f + 0.075f, 0.2f, 0.05f), 3f, false)
      .withColor(1, 1, 1)
  )
  private val sizeTF =
    new TextField(LocationInfo.from16x9(0.3f, 0.55f, 0.4f, 0.075f), maxFontSize = 2.5f)
  addComponent(sizeTF)

  addComponent(
    new Label("World seed", LocationInfo.from16x9(0.3f, 0.4f + 0.075f, 0.2f, 0.05f), 3f, false)
      .withColor(1, 1, 1)
  )
  private val seedTF = new TextField(LocationInfo.from16x9(0.3f, 0.4f, 0.4f, 0.075f), maxFontSize = 2.5f)
  addComponent(seedTF)

  addComponent(Button("Cancel", LocationInfo.from16x9(0.3f, 0.05f, 0.19f, 0.1f)) {
    onEvent(Event.GoBack)
  })
  addComponent(Button("Create world", LocationInfo.from16x9(0.51f, 0.05f, 0.19f, 0.1f))(createWorld()))

  private def createWorld(): Unit = {
    try {
      val baseFolder = new File(saveFolder, "saves")
      val file = uniqueFile(baseFolder, cleanupFileName(nameTF.text))
      val size = sizeTF.text.toByteOption.filter(s => s >= 0 && s <= 20)
      val seed = Some(seedTF.text)
        .filter(_.nonEmpty)
        .map(s => s.toLongOption.getOrElse(new Random(s.##.toLong << 32 | s.reverse.##).nextLong()))

      onEvent(Event.StartGame(file, WorldSettings(Some(nameTF.text), size, seed)))
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
