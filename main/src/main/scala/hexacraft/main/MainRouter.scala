package hexacraft.main

import hexacraft.client.BlockTextureLoader
import hexacraft.game.GameKeyboard
import hexacraft.gui.Scene
import hexacraft.infra.audio.AudioSystem
import hexacraft.infra.fs.FileSystem
import hexacraft.infra.window.CursorMode
import hexacraft.main.Menus.ChoosePlayerNameMenu
import hexacraft.nbt.Nbt
import hexacraft.server.WorldProviderFromFile
import hexacraft.util.{Channel, Result}
import hexacraft.world.{CylinderSize, WorldGenSettings, WorldInfo}

import java.io.File
import java.util.UUID
import scala.collection.mutable.ArrayBuffer

object MainRouter {
  enum Event {
    case ChangeScene(route: SceneRoute)
    case QuitRequested
  }
}

class MainRouter(
    saveFolder: File,
    multiplayerEnabled: Boolean,
    fs: FileSystem,
    window: GameWindow,
    kb: GameKeyboard,
    audioSystem: AudioSystem
) {

  // TODO: persist this state somewhere (in a file probably)
  private val servers: ArrayBuffer[(String, Int)] = ArrayBuffer.empty

  def route(sceneRoute: SceneRoute): (Scene, Channel.Receiver[MainRouter.Event]) = {
    import MainRouter.Event

    val (tx, rx) = Channel[Event]()
    val scene = createScene(
      sceneRoute,
      r => tx.send(Event.ChangeScene(r)),
      () => tx.send(Event.QuitRequested)
    )

    (scene, rx)
  }

  private def createScene(
      sceneRoute: SceneRoute,
      route: SceneRoute => Unit,
      requestQuit: () => Unit
  ): Scene = sceneRoute match {
    case SceneRoute.Main =>
      import Menus.MainMenu.Event

      val (scene, rx) = Menus.MainMenu.create(multiplayerEnabled)

      rx.onEvent {
        case Event.Play        => route(SceneRoute.WorldChooser)
        case Event.Multiplayer => route(SceneRoute.Multiplayer)
        case Event.Settings    => route(SceneRoute.Settings)
        case Event.Quit        => requestQuit()
      }

      scene

    case SceneRoute.WorldChooser =>
      import Menus.WorldChooserMenu.Event

      val (scene, rx) = Menus.WorldChooserMenu.create(saveFolder, fs)

      rx.onEvent {
        case Event.StartGame(saveDir) =>
          route(SceneRoute.Game(saveDir, isHosting = true, isOnline = false, null))
        case Event.CreateNewWorld => route(SceneRoute.NewWorld)
        case Event.GoBack         => route(SceneRoute.Main)
      }

      scene

    case SceneRoute.NewWorld =>
      import Menus.NewWorldMenu.Event

      val (scene, rx) = Menus.NewWorldMenu.create(saveFolder)

      rx.onEvent {
        case Event.StartGame(saveDir, worldName, worldSize, worldSeed) =>
          val worldInfo = WorldInfo(
            0,
            worldName,
            CylinderSize(worldSize),
            WorldGenSettings.fromSeed(worldSeed)
          )
          val worldProvider = WorldProviderFromFile(saveDir, fs)
          worldProvider.saveWorldData(Nbt.encode(worldInfo))

          route(SceneRoute.Game(saveDir, isHosting = true, isOnline = false, null))
        case Event.GoBack => route(SceneRoute.WorldChooser)
      }

      scene

    case SceneRoute.Multiplayer =>
      import Menus.MultiplayerMenu.Event

      val (scene, rx) = Menus.MultiplayerMenu.create()

      rx.onEvent {
        case Event.Join   => route(SceneRoute.JoinWorld)
        case Event.Host   => route(SceneRoute.HostWorld)
        case Event.GoBack => route(SceneRoute.Main)
      }

      scene

    case SceneRoute.JoinWorld =>
      import Menus.JoinWorldChooserMenu.Event

      val (scene, rx) = Menus.JoinWorldChooserMenu.create(servers.toSeq)

      rx.onEvent {
        case Event.Join(address, port) =>
          println(s"Will connect to: $address at port $port")
          route(
            SceneRoute.ChoosePlayerName(
              SceneRoute.Game(
                null,
                isHosting = false,
                isOnline = true,
                (address, port)
              )
            )
          )
        case Event.AddServer => route(SceneRoute.AddServer)
        case Event.GoBack    => route(SceneRoute.Multiplayer)
      }

      scene

    case SceneRoute.AddServer =>
      import Menus.AddServerMenu.Event

      val (scene, rx) = Menus.AddServerMenu.create()

      rx.onEvent {
        case Event.AddServer(address, port) =>
          servers += address -> port
          route(SceneRoute.JoinWorld)
        case Event.GoBack =>
          route(SceneRoute.JoinWorld)
      }

      scene

    case SceneRoute.HostWorld =>
      import Menus.HostWorldChooserMenu.Event

      val (scene, rx) = Menus.HostWorldChooserMenu.create(saveFolder, fs)

      rx.onEvent {
        case Event.Host(f) =>
          println(s"Hosting world from ${f.saveFile.getName}")
          route(
            SceneRoute.ChoosePlayerName(
              SceneRoute.Game(f.saveFile, isHosting = true, isOnline = true, null)
            )
          )
        case Event.GoBack => route(SceneRoute.Multiplayer)
      }

      scene

    case SceneRoute.ChoosePlayerName(next) =>
      import Menus.ChoosePlayerNameMenu.Event

      loadUserName() match {
        case Some(_) =>
          createScene(next, route, requestQuit)
        case None =>
          val (scene, rx) = ChoosePlayerNameMenu.create()
          rx.onEvent {
            case Event.ChooseName(name) =>
              storeUserName(name)
              route(next)
            case Event.Cancel =>
              route(SceneRoute.Multiplayer)
          }
          scene
      }

    case SceneRoute.Settings =>
      new Menus.SettingsMenu(() => route(SceneRoute.Main))

    case SceneRoute.Game(saveDir, isHosting, isOnline, serverLocation) =>
      val (serverIp, serverPort) = Option(serverLocation)
        .getOrElse(("127.0.0.1", (Math.random() * 8000 + 2000).toInt))

      val client = GameScene.ClientParams(
        loadUserId(),
        loadUserName().getOrElse(""),
        serverIp,
        serverPort,
        isOnline,
        kb,
        BlockTextureLoader.instance,
        audioSystem,
        window.windowSize
      )
      val server = if isHosting then {
        val worldProvider = WorldProviderFromFile(saveDir, fs)
        val worldInfo = Nbt.decode[WorldInfo](worldProvider.loadWorldData().get).get
        Some(GameScene.ServerParams(worldInfo, worldProvider))
      } else {
        None
      }
      GameScene.create(client, server) match {
        case Result.Ok(res) =>
          val (scene, rx) = res

          rx.onEvent {
            case GameScene.Event.GameQuit =>
              route(SceneRoute.Main)
              System.gc()
            case GameScene.Event.CursorCaptured =>
              window.setCursorMode(CursorMode.Disabled)
            case GameScene.Event.CursorReleased =>
              window.setCursorMode(CursorMode.Normal)
          }

          scene
        case Result.Err(message) =>
          println(message)
          createScene(SceneRoute.Main, route, requestQuit)
      }
  }

  private def loadUserId(): UUID = {
    val path = new File(saveFolder, "userid.txt").toPath
    val userId = fs.readAllBytes(path) match {
      case Result.Ok(bytes) => UUID.fromString(String(bytes))
      case Result.Err(e) =>
        e match {
          case FileSystem.Error.FileNotFound =>
            val userId = UUID.randomUUID()
            fs.writeBytes(path, userId.toString.getBytes)
            userId
          case FileSystem.Error.Unknown(e) =>
            throw new RuntimeException(s"Failed to read userid file", e)
        }
    }
    userId
  }

  private def loadUserName(): Option[String] = {
    val path = new File(saveFolder, "username.txt").toPath
    fs.readAllBytes(path) match {
      case Result.Ok(bytes) => Some(String(bytes))
      case Result.Err(e) =>
        e match {
          case FileSystem.Error.FileNotFound => None
          case FileSystem.Error.Unknown(e) =>
            throw new RuntimeException(s"Failed to read username file", e)
        }
    }
  }

  private def storeUserName(name: String): Unit = {
    val path = new File(saveFolder, "username.txt").toPath
    fs.writeBytes(path, name.getBytes)
  }
}
