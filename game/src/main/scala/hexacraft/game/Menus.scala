package hexacraft.game

import hexacraft.gui.{LocationInfo, RenderContext, Scene}
import hexacraft.gui.comp.{Button, Component, GUITransformation, Label, ScrollPane, SubComponents, TextField}
import hexacraft.infra.fs.{FileSystem, NbtIO}
import hexacraft.renderer.TextureSingle
import hexacraft.world.WorldSettings

import com.martomate.nbt.Nbt

import java.io.File
import java.nio.file.Path
import scala.util.Random

object Menus {

  abstract class MenuScene extends Scene with SubComponents {
    override def render(transformation: GUITransformation)(using context: RenderContext): Unit = {
      Component.drawImage(
        LocationInfo(-context.windowAspectRatio, -1, context.windowAspectRatio * 2, 2),
        transformation.x,
        transformation.y,
        TextureSingle.getTexture("textures/gui/menu/background"),
        context.windowAspectRatio
      )
      super.render(transformation)
    }
  }

  object WorldInfo {
    def fromFile(saveFile: File, fs: FileSystem): WorldInfo = {
      val nbtFile = saveFile.toPath.resolve("world.dat")
      val io = new NbtIO(fs)

      val existingName =
        for
          (_, tag) <- io.loadTag(nbtFile.toFile)
          general <- tag.getMap("general")
          name <- general.getString("name")
        yield name

      WorldInfo(saveFile, existingName.getOrElse(saveFile.getName))
    }
  }

  case class WorldInfo(saveFile: File, name: String)

  object MainMenu {
    enum Event:
      case Play
      case Multiplayer
      case Settings
      case Quit
  }

  class MainMenu(multiplayerEnabled: Boolean)(onEvent: MainMenu.Event => Unit) extends MenuScene {
    import MainMenu.Event

    addComponent(new Label("Hexacraft", LocationInfo.from16x9(0, 0.8f, 1, 0.2f), 10).withColor(1, 1, 1))
    addComponent(Button("Play", LocationInfo.from16x9(0.4f, 0.55f, 0.2f, 0.1f))(onEvent(Event.Play)))

    if multiplayerEnabled then
      addComponent(Button("Multiplayer", LocationInfo.from16x9(0.4f, 0.4f, 0.2f, 0.1f))(onEvent(Event.Multiplayer)))

    addComponent(
      Button("Settings", LocationInfo.from16x9(0.4f, if multiplayerEnabled then 0.25f else 0.4f, 0.2f, 0.1f))(
        onEvent(Event.Settings)
      )
    )
    addComponent(Button("Quit", LocationInfo.from16x9(0.4f, 0.05f, 0.2f, 0.1f))(onEvent(Event.Quit)))
  }

  object HostWorldChooserMenu {
    enum Event:
      case Host(worldInfo: WorldInfo)
      case GoBack
  }

  class HostWorldChooserMenu(saveFolder: File, fs: FileSystem)(onEvent: HostWorldChooserMenu.Event => Unit)
      extends MenuScene {

    import HostWorldChooserMenu.Event

    addComponent(new Label("Choose world", LocationInfo.from16x9(0, 0.85f, 1, 0.15f), 6).withColor(1, 1, 1))

    private val scrollPane = new ScrollPane(LocationInfo.from16x9(0.285f, 0.225f, 0.43f, 0.635f), 0.025f * 2)

    for (f, i) <- getWorlds.zipWithIndex
    do
      scrollPane.addComponent(
        Button(f.name, LocationInfo.from16x9(0.3f, 0.75f - 0.1f * i, 0.4f, 0.075f)) {
          onEvent(Event.Host(f))
          // TODO: the network manager should repeatedly connect to the server registry.
          //  This will be blocking until a client wants to connect or after a timeout
          //  If this is not done in a certain time period the server will be deregistered from the server registry
        }
      )
    addComponent(scrollPane)

    addComponent(Button("Back to menu", LocationInfo.from16x9(0.3f, 0.05f, 0.4f, 0.1f))(onEvent(Event.GoBack)))

    private def getWorlds: Seq[WorldInfo] =
      val baseFolder = new File(saveFolder, "saves")
      if baseFolder.exists() then
        baseFolder
          .listFiles()
          .filter(f => new File(f, "world.dat").exists())
          .map(saveFile => WorldInfo.fromFile(saveFile, fs))
          .toSeq
      else Seq.empty[WorldInfo]

  }

  object JoinWorldChooserMenu {
    enum Event:
      case Join(address: String, port: Int)
      case GoBack

    private case class OnlineWorldInfo(id: Long, name: String, description: String)

    private case class OnlineWorldConnectionDetails(address: String, port: Int, time: Long)
  }

  class JoinWorldChooserMenu(onEvent: JoinWorldChooserMenu.Event => Unit) extends MenuScene {

    import JoinWorldChooserMenu.*

    addComponent(new Label("Choose world", LocationInfo.from16x9(0, 0.85f, 1, 0.15f), 6).withColor(1, 1, 1))
    private val scrollPane = new ScrollPane(LocationInfo.from16x9(0.285f, 0.225f, 0.43f, 0.635f), 0.025f * 2)
    addComponent(scrollPane)

    addComponent(Button("Back to menu", LocationInfo.from16x9(0.3f, 0.05f, 0.4f, 0.1f))(onEvent(Event.GoBack)))

    updateServerList()

    private def updateServerList(): Unit =
      for (f, i) <- getWorlds.zipWithIndex
      do
        scrollPane.addComponent(
          Button(f.name, LocationInfo.from16x9(0.3f, 0.75f - 0.1f * i, 0.4f, 0.075f)) {
            val connectionDetails = loadOnlineWorld(f.id)
            onEvent(Event.Join(connectionDetails.address, connectionDetails.port))
          }
        )

    private def getWorlds: Seq[OnlineWorldInfo] =
      Seq(
        OnlineWorldInfo(new Random().nextLong(), "Test Online World", "Welcome to my test world!"),
        OnlineWorldInfo(new Random().nextLong(), "Another Online World", "Free bitcakes!")
      )

    private def loadOnlineWorld(id: Long): OnlineWorldConnectionDetails =
      // TODO: connect to the server registry to get this information
      OnlineWorldConnectionDetails(
        "localhost",
        1234,
        System.currentTimeMillis() + 10
      )
  }

  object MultiplayerMenu {
    enum Event:
      case Join
      case Host
      case GoBack
  }

  class MultiplayerMenu(onEvent: MultiplayerMenu.Event => Unit) extends MenuScene {
    import MultiplayerMenu.Event

    addComponent(new Label("Multiplayer", LocationInfo.from16x9(0, 0.8f, 1, 0.2f), 10).withColor(1, 1, 1))
    addComponent(Button("Join", LocationInfo.from16x9(0.4f, 0.55f, 0.2f, 0.1f))(onEvent(Event.Join)))
    addComponent(Button("Host", LocationInfo.from16x9(0.4f, 0.4f, 0.2f, 0.1f))(onEvent(Event.Host)))
    addComponent(Button("Back", LocationInfo.from16x9(0.4f, 0.05f, 0.2f, 0.1f))(onEvent(Event.GoBack)))
  }

  class SettingsMenu(onBack: () => Unit) extends MenuScene {
    addComponent(Button("Coming soon!", LocationInfo.from16x9(0.4f, 0.55f, 0.2f, 0.1f)) {
      println("Settings will be implemented soon")
    })
    addComponent(Button("Back to menu", LocationInfo.from16x9(0.4f, 0.25f, 0.2f, 0.1f)) {
      onBack()
    })
  }

  object WorldChooserMenu {
    enum Event:
      case StartGame(saveDir: File, settings: WorldSettings)
      case CreateNewWorld
      case GoBack
  }

  class WorldChooserMenu(saveFolder: File, fs: FileSystem)(onEvent: WorldChooserMenu.Event => Unit) extends MenuScene {
    import WorldChooserMenu.Event

    addComponent(
      new Label("Choose world", LocationInfo.from16x9(0, 0.85f, 1, 0.15f), 6).withColor(1, 1, 1)
    )

    addComponent(makeScrollPane)

    addComponent(Button("Back to menu", LocationInfo.from16x9(0.3f, 0.05f, 0.19f, 0.1f)) {
      onEvent(Event.GoBack)
    })
    addComponent(Button("New world", LocationInfo.from16x9(0.51f, 0.05f, 0.19f, 0.1f)) {
      onEvent(Event.CreateNewWorld)
    })

    private def makeScrollPane: ScrollPane = {
      val scrollPaneLocation = LocationInfo.from16x9(0.3f, 0.25f, 0.4f, 0.575f).expand(0.025f * 2)
      val scrollPane = new ScrollPane(scrollPaneLocation, 0.025f * 2)

      val buttons = for ((f, i) <- getWorlds.zipWithIndex) yield makeWorldButton(f, i)
      for (b <- buttons) scrollPane.addComponent(b)

      scrollPane
    }

    private def makeWorldButton(world: WorldInfo, listIndex: Int): Button = {
      val buttonLocation = LocationInfo.from16x9(0.3f, 0.75f - 0.1f * listIndex, 0.4f, 0.075f)

      Button(world.name, buttonLocation) {
        onEvent(Event.StartGame(world.saveFile, WorldSettings.none))
      }
    }

    private def getWorlds: Seq[WorldInfo] = {
      val baseFolder = new File(saveFolder, "saves")
      if (fs.exists(baseFolder.toPath)) {
        for (saveFile <- saveFoldersSortedBy(baseFolder, p => -fs.lastModified(p).toEpochMilli))
          yield WorldInfo.fromFile(saveFile.toFile, fs)
      } else {
        Seq.empty[WorldInfo]
      }
    }

    private def saveFoldersSortedBy[S](baseFolder: File, sortFunc: Path => S)(using
        Ordering[S]
    ): Seq[Path] = {
      fs.listFiles(baseFolder.toPath)
        .map(worldFolder => (worldFolder, worldFolder.resolve("world.dat")))
        .filter(t => fs.exists(t._2))
        .sortBy(t => sortFunc(t._2))
        .map(_._1)
    }
  }

  object NewWorldMenu {
    enum Event:
      case StartGame(saveDir: File, settings: WorldSettings)
      case GoBack
  }

  class NewWorldMenu(saveFolder: File)(onEvent: NewWorldMenu.Event => Unit) extends MenuScene {
    import NewWorldMenu.Event

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

}
