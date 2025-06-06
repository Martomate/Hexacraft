package hexacraft.main

import hexacraft.client.GameClientSocket
import hexacraft.game.NetworkPacket
import hexacraft.gui.{LocationInfo, RenderContext, Scene}
import hexacraft.gui.comp.*
import hexacraft.infra.fs.{FileSystem, NbtIO}
import hexacraft.nbt.Nbt
import hexacraft.renderer.TextureSingle
import hexacraft.util.Channel
import hexacraft.world.WorldSettings

import java.io.File
import java.net.InetAddress
import java.nio.file.Path
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Random, Try}

object Menus {
  private val backgroundImage = TextureSingle.getTexture("textures/gui/menu/background")

  abstract class MenuScene extends Scene with SubComponents {
    override def render(context: RenderContext): Unit = {
      val location = LocationInfo(-context.windowAspectRatio, -1, context.windowAspectRatio * 2, 2)
      Component.drawImage(location, context.offset.x, context.offset.y, backgroundImage, context.windowAspectRatio)
      super.render(context)
    }
  }

  object WorldInfo {
    def fromFile(saveFile: File, fs: FileSystem): WorldInfo = {
      val nbtFile = saveFile.toPath.resolve("world.dat")
      val io = new NbtIO(fs)

      val existingName =
        for {
          (_, tag) <- io.loadTag(nbtFile.toFile)
          general <- tag.getMap("general")
          name <- general.getString("name")
        } yield name

      WorldInfo(saveFile, existingName.getOrElse(saveFile.getName))
    }
  }

  case class WorldInfo(saveFile: File, name: String)

  // These classes exist because the router tests use an "instance of" check. Those tests should really be checking for something else.
  class MainMenu private extends MenuScene
  class HostWorldChooserMenu private extends MenuScene
  class JoinWorldChooserMenu private extends MenuScene
  class AddServerMenu private extends MenuScene
  class ChoosePlayerNameMenu private extends MenuScene
  class MultiplayerMenu private extends MenuScene
  class WorldChooserMenu private extends MenuScene
  class NewWorldMenu private extends MenuScene

  // Below are the factories for many of the menus in the game
  object MainMenu {
    enum Event {
      case Play
      case Multiplayer
      case Settings
      case Quit
    }

    def create(multiplayerEnabled: Boolean): (MainMenu, Channel.Receiver[Event]) = {
      val (tx, rx) = Channel[Event]()

      val menu = new MainMenu

      menu.addComponent(new Label("Hexacraft", LocationInfo.from16x9(0, 0.8f, 1, 0.2f), 10).withColor(1, 1, 1))
      menu.addComponent(Button("Play", LocationInfo.from16x9(0.4f, 0.55f, 0.2f, 0.1f))(tx.send(Event.Play)))

      if multiplayerEnabled then {
        menu.addComponent(
          Button("Multiplayer", LocationInfo.from16x9(0.4f, 0.4f, 0.2f, 0.1f))(tx.send(Event.Multiplayer))
        )
      }

      menu.addComponent(
        Button("Settings", LocationInfo.from16x9(0.4f, if multiplayerEnabled then 0.25f else 0.4f, 0.2f, 0.1f))(
          tx.send(Event.Settings)
        )
      )
      menu.addComponent(Button("Quit", LocationInfo.from16x9(0.4f, 0.05f, 0.2f, 0.1f))(tx.send(Event.Quit)))

      (menu, rx)
    }
  }

  object HostWorldChooserMenu {
    enum Event {
      case Host(worldInfo: WorldInfo)
      case GoBack
    }

    def create(saveFolder: File, fs: FileSystem): (MenuScene, Channel.Receiver[Event]) = {
      val (tx, rx) = Channel[Event]()

      val menu = new HostWorldChooserMenu

      val worlds = getWorlds(saveFolder, fs)
      val scrollPane = makeScrollPane(worlds, tx)

      menu.addComponent(new Label("Choose world", LocationInfo.from16x9(0, 0.85f, 1, 0.15f), 6).withColor(1, 1, 1))
      menu.addComponent(scrollPane)
      menu.addComponent(Button("Back to menu", LocationInfo.from16x9(0.3f, 0.05f, 0.4f, 0.1f))(tx.send(Event.GoBack)))

      (menu, rx)
    }

    private def makeScrollPane(worlds: Seq[WorldInfo], tx: Channel.Sender[Event]) = {
      val scrollPane = new ScrollPane(LocationInfo.from16x9(0.285f, 0.225f, 0.43f, 0.635f), 0.025f * 2)

      for (f, i) <- worlds.zipWithIndex do {
        scrollPane.addComponent(
          Button(f.name, LocationInfo.from16x9(0.3f, 0.75f - 0.1f * i, 0.4f, 0.075f)) {
            tx.send(Event.Host(f))
            // TODO: the network manager should repeatedly connect to the server registry.
            //  This will be blocking until a client wants to connect or after a timeout
            //  If this is not done in a certain time period the server will be deregistered from the server registry
          }
        )
      }

      scrollPane
    }
  }

  object JoinWorldChooserMenu {
    enum Event {
      case Join(address: String, port: Int)
      case AddServer
      case GoBack
    }

    def create(servers: Seq[(String, Int)]): (MenuScene, Channel.Receiver[Event]) = {
      val (tx, rx) = Channel[Event]()

      val scrollPane = new ScrollPane(LocationInfo.from16x9(0.285f, 0.225f, 0.43f, 0.635f), 0.025f * 2)

      enum ServerState {
        case Connecting
        case Unavailable
        case Available(name: String)
      }

      val serverConnectionState = ArrayBuffer.fill[ServerState](servers.length)(ServerState.Connecting)

      val stateUpdates = mutable.Queue.empty[(Int, ServerState)]

      for ((address, port), idx) <- servers.zipWithIndex do {
        Future {
          val socket = GameClientSocket(address, port)
          val res = for {
            res <- Try(socket.sendPacketAndWait(NetworkPacket.GetWorldInfo).asMap.get).toOption
            general <- res.getMap("general")
            name <- general.getString("name")
          } yield ServerState.Available(name)
          socket.close()
          stateUpdates.synchronized {
            stateUpdates += idx -> res.getOrElse(ServerState.Unavailable)
          }
        }
      }

      def updateScrollPane(): Unit = {
        for (((address, port), state), i) <- servers.zip(serverConnectionState).zipWithIndex do {
          val (ready, title) = state match {
            case ServerState.Connecting      => (false, s"Connecting to $address:$port...")
            case ServerState.Unavailable     => (false, "Connection failed")
            case ServerState.Available(name) => (true, name)
          }
          val buttonBounds = LocationInfo.from16x9(0.3f, 0.75f - 0.1f * i, 0.4f, 0.075f)
          val button = Button(title, buttonBounds, disabled = !ready) {
            tx.send(Event.Join(address, port))
          }
          scrollPane.replaceComponent(i, button)
        }
      }

      updateScrollPane()

      val menu = new JoinWorldChooserMenu

      menu.onTick {
        if stateUpdates.nonEmpty then {
          val updates = stateUpdates.synchronized {
            stateUpdates.removeAll()
          }

          for (idx, state) <- updates do {
            serverConnectionState(idx) = state
            updateScrollPane()
          }
        }
      }

      menu.addComponent(new Label("Choose world", LocationInfo.from16x9(0, 0.85f, 1, 0.15f), 6).withColor(1, 1, 1))
      menu.addComponent(scrollPane)
      menu.addComponent(Button("Back", LocationInfo.from16x9(0.3f, 0.05f, 0.19f, 0.1f))(tx.send(Event.GoBack)))
      menu.addComponent(Button("Add", LocationInfo.from16x9(0.51f, 0.05f, 0.19f, 0.1f))(tx.send(Event.AddServer)))

      (menu, rx)
    }
  }

  object AddServerMenu {
    enum Event {
      case AddServer(address: String, port: Int)
      case GoBack
    }

    def create(): (MenuScene, Channel.Receiver[Event]) = {
      val (tx, rx) = Channel[Event]()

      val menu = new AddServerMenu

      val addressTF = new TextField(LocationInfo.from16x9(0.3f, 0.7f, 0.4f, 0.075f), maxFontSize = 2.5f)
      val portTF = new TextField(LocationInfo.from16x9(0.3f, 0.55f, 0.4f, 0.075f), maxFontSize = 2.5f)

      def calculateResult = {
        Try {
          val address = addressTF.text
          InetAddress.getByName(address)
          val port = portTF.text.toIntOption.filter(p => p >= 0 && p <= 65535).get
          Event.AddServer(address, port)
        }.toOption
      }

      val addressLabel = new Label("IP address", LocationInfo.from16x9(0.3f, 0.7f + 0.075f, 0.2f, 0.05f), 3f, false)
        .withColor(1, 1, 1)
      val portLabel = new Label("Port", LocationInfo.from16x9(0.3f, 0.55f + 0.075f, 0.2f, 0.05f), 3f, false)
        .withColor(1, 1, 1)

      menu.addComponent(addressLabel)
      menu.addComponent(addressTF)

      menu.addComponent(portLabel)
      menu.addComponent(portTF)

      menu.addComponent(Button("Back", LocationInfo.from16x9(0.3f, 0.05f, 0.19f, 0.1f))(tx.send(Event.GoBack)))
      menu.addComponent(Button("Add", LocationInfo.from16x9(0.51f, 0.05f, 0.19f, 0.1f)) {
        calculateResult.foreach(tx.send)
      })

      (menu, rx)
    }
  }

  object ChoosePlayerNameMenu {
    enum Event {
      case ChooseName(name: String)
      case Cancel
    }

    def create(): (MenuScene, Channel.Receiver[Event]) = {
      val (tx, rx) = Channel[Event]()

      val menu = new ChoosePlayerNameMenu

      val nameTF = new TextField(LocationInfo.from16x9(0.3f, 0.7f, 0.4f, 0.075f), maxFontSize = 2.5f)

      val nameLabel = new Label("Player name", LocationInfo.from16x9(0.3f, 0.7f + 0.075f, 0.2f, 0.05f), 3f, false)
        .withColor(1, 1, 1)

      menu.addComponent(nameLabel)
      menu.addComponent(nameTF)

      menu.addComponent(Button("Cancel", LocationInfo.from16x9(0.3f, 0.05f, 0.19f, 0.1f))(tx.send(Event.Cancel)))
      menu.addComponent(Button("Choose name", LocationInfo.from16x9(0.51f, 0.05f, 0.19f, 0.1f)) {
        if nameTF.text.nonEmpty then {
          tx.send(Event.ChooseName(nameTF.text))
        }
      })

      (menu, rx)
    }
  }

  object MultiplayerMenu {
    enum Event {
      case Join
      case Host
      case GoBack
    }

    def create(): (MultiplayerMenu, Channel.Receiver[Event]) = {
      val (tx, rx) = Channel[Event]()

      val menu = new MultiplayerMenu
      menu.addComponent(new Label("Multiplayer", LocationInfo.from16x9(0, 0.8f, 1, 0.2f), 10).withColor(1, 1, 1))
      menu.addComponent(Button("Join", LocationInfo.from16x9(0.4f, 0.55f, 0.2f, 0.1f))(tx.send(Event.Join)))
      menu.addComponent(Button("Host", LocationInfo.from16x9(0.4f, 0.4f, 0.2f, 0.1f))(tx.send(Event.Host)))
      menu.addComponent(Button("Back", LocationInfo.from16x9(0.4f, 0.05f, 0.2f, 0.1f))(tx.send(Event.GoBack)))

      (menu, rx)
    }
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
    enum Event {
      case StartGame(saveDir: File, settings: WorldSettings)
      case CreateNewWorld
      case GoBack
    }

    def create(saveFolder: File, fs: FileSystem): (WorldChooserMenu, Channel.Receiver[Event]) = {
      val (tx, rx) = Channel[Event]()
      val menu = new WorldChooserMenu

      val worlds = getWorlds(saveFolder, fs)
      val scrollPane = makeScrollPane(worlds, tx)

      menu.addComponent(
        new Label("Choose world", LocationInfo.from16x9(0, 0.85f, 1, 0.15f), 6).withColor(1, 1, 1)
      )
      menu.addComponent(scrollPane)
      menu.addComponent(Button("Back to menu", LocationInfo.from16x9(0.3f, 0.05f, 0.19f, 0.1f)) {
        tx.send(Event.GoBack)
      })
      menu.addComponent(Button("New world", LocationInfo.from16x9(0.51f, 0.05f, 0.19f, 0.1f)) {
        tx.send(Event.CreateNewWorld)
      })

      (menu, rx)
    }

    private def makeScrollPane(worlds: Seq[WorldInfo], tx: Channel.Sender[Event]): ScrollPane = {
      val scrollPaneLocation = LocationInfo.from16x9(0.3f, 0.25f, 0.4f, 0.575f).expand(0.025f * 2)
      val scrollPane = new ScrollPane(scrollPaneLocation, 0.025f * 2)

      val buttons = for (f, i) <- worlds.zipWithIndex yield makeWorldButton(f, i, tx)
      for b <- buttons do {
        scrollPane.addComponent(b)
      }

      scrollPane
    }

    private def makeWorldButton(world: WorldInfo, listIndex: Int, tx: Channel.Sender[Event]): Button = {
      val buttonLocation = LocationInfo.from16x9(0.3f, 0.75f - 0.1f * listIndex, 0.4f, 0.075f)

      Button(world.name, buttonLocation) {
        tx.send(Event.StartGame(world.saveFile, WorldSettings.none))
      }
    }
  }

  private def getWorlds(saveFolder: File, fs: FileSystem): Seq[WorldInfo] = {
    val baseFolder = new File(saveFolder, "saves")
    if fs.exists(baseFolder.toPath) then {
      for saveFile <- saveFoldersSortedBy(fs, baseFolder, p => -fs.lastModified(p).toEpochMilli) yield {
        WorldInfo.fromFile(saveFile.toFile, fs)
      }
    } else {
      Seq.empty[WorldInfo]
    }
  }

  private def saveFoldersSortedBy[S](fs: FileSystem, baseFolder: File, sortFunc: Path => S)(using
      Ordering[S]
  ): Seq[Path] = {
    fs.listFiles(baseFolder.toPath)
      .map(worldFolder => (worldFolder, worldFolder.resolve("world.dat")))
      .filter(t => fs.exists(t._2))
      .sortBy(t => sortFunc(t._2))
      .map(_._1)
  }

  object NewWorldMenu {
    enum Event {
      case StartGame(saveDir: File, settings: WorldSettings)
      case GoBack
    }

    def create(saveFolder: File): (NewWorldMenu, Channel.Receiver[Event]) = {
      val (tx, rx) = Channel[Event]()
      val menu = new NewWorldMenu

      val nameTF = new TextField(LocationInfo.from16x9(0.3f, 0.7f, 0.4f, 0.075f), maxFontSize = 2.5f)
      val sizeTF = new TextField(LocationInfo.from16x9(0.3f, 0.55f, 0.4f, 0.075f), maxFontSize = 2.5f)
      val seedTF = new TextField(LocationInfo.from16x9(0.3f, 0.4f, 0.4f, 0.075f), maxFontSize = 2.5f)

      val createWorld = () => {
        try {
          val baseFolder = new File(saveFolder, "saves")
          val file = uniqueFile(baseFolder, cleanupFileName(nameTF.text))
          val size = sizeTF.text.toByteOption.filter(s => s >= 0 && s <= 20)
          val seed = Some(seedTF.text)
            .filter(_.nonEmpty)
            .map(s => s.toLongOption.getOrElse(new Random(s.##.toLong << 32 | s.reverse.##).nextLong()))

          tx.send(Event.StartGame(file, WorldSettings(Some(nameTF.text), size, seed)))
        } catch {
          case _: Exception =>
          // TODO: complain about the input
        }
      }

      val nameLabel = new Label("World name", LocationInfo.from16x9(0.3f, 0.7f + 0.075f, 0.2f, 0.05f), 3f, false)
        .withColor(1, 1, 1)
      val sizeLabel = new Label("World size", LocationInfo.from16x9(0.3f, 0.55f + 0.075f, 0.2f, 0.05f), 3f, false)
        .withColor(1, 1, 1)
      val seedLabel = new Label("World seed", LocationInfo.from16x9(0.3f, 0.4f + 0.075f, 0.2f, 0.05f), 3f, false)
        .withColor(1, 1, 1)

      menu.addComponent(nameLabel)
      menu.addComponent(nameTF)

      menu.addComponent(sizeLabel)
      menu.addComponent(sizeTF)

      menu.addComponent(seedLabel)
      menu.addComponent(seedTF)

      menu.addComponent(Button("Cancel", LocationInfo.from16x9(0.3f, 0.05f, 0.19f, 0.1f)) {
        tx.send(Event.GoBack)
      })
      menu.addComponent(Button("Create world", LocationInfo.from16x9(0.51f, 0.05f, 0.19f, 0.1f)) {
        createWorld()
      })

      (menu, rx)
    }

    private def uniqueFile(baseFolder: File, fileName: String): File = {
      var file: File = null
      var count = 0
      while {
        count += 1
        val name = if count == 1 then fileName else fileName + " " + count
        file = new File(baseFolder, name)
        file.exists()
      } do ()

      file
    }

    private def cleanupFileName(fileName: String): String = {
      def charValid(c: Char): Boolean = {
        c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == ' '
      }

      val name = fileName.map(c => if charValid(c) then c else '_').trim
      if name.nonEmpty then name else "New World"
    }
  }
}
