package hexacraft.main

import hexacraft.game.*
import hexacraft.gui.Scene
import hexacraft.infra.audio.AudioSystem
import hexacraft.infra.fs.FileSystem
import hexacraft.infra.window.CursorMode
import hexacraft.util.Channel
import hexacraft.world.WorldSettings

import java.io.File

object MainRouter {
  enum Event {
    case SceneChanged(newScene: Scene)
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
)(eventListener: Channel.Sender[MainRouter.Event]) {

  def route(sceneRoute: SceneRoute): Unit = {
    eventListener.send(MainRouter.Event.SceneChanged(createScene(sceneRoute)))
  }

  private def createScene(sceneRoute: SceneRoute): Scene = sceneRoute match {
    case SceneRoute.Main =>
      import Menus.MainMenu.Event

      val (tx, rx) = Channel[Event]()
      val scene = Menus.MainMenu(multiplayerEnabled)(tx)

      rx.onEvent {
        case Event.Play        => route(SceneRoute.WorldChooser)
        case Event.Multiplayer => route(SceneRoute.Multiplayer)
        case Event.Settings    => route(SceneRoute.Settings)
        case Event.Quit        => eventListener.send(MainRouter.Event.QuitRequested)
      }

      scene

    case SceneRoute.WorldChooser =>
      import Menus.WorldChooserMenu.Event

      val (tx, rx) = Channel[Event]()
      val scene = Menus.WorldChooserMenu(saveFolder, fs)(tx)

      rx.onEvent {
        case Event.StartGame(saveDir, settings) =>
          route(SceneRoute.Game(saveDir, settings, isHosting = true, isOnline = false, null))
        case Event.CreateNewWorld => route(SceneRoute.NewWorld)
        case Event.GoBack         => route(SceneRoute.Main)
      }

      scene

    case SceneRoute.NewWorld =>
      import Menus.NewWorldMenu.Event

      val (tx, rx) = Channel[Event]()
      val scene = Menus.NewWorldMenu(saveFolder)(tx)

      rx.onEvent {
        case Event.StartGame(saveDir, settings) =>
          route(SceneRoute.Game(saveDir, settings, isHosting = true, isOnline = false, null))
        case Event.GoBack => route(SceneRoute.WorldChooser)
      }

      scene

    case SceneRoute.Multiplayer =>
      import Menus.MultiplayerMenu.Event

      val (tx, rx) = Channel[Event]()
      val scene = Menus.MultiplayerMenu(tx)

      rx.onEvent {
        case Event.Join   => route(SceneRoute.JoinWorld)
        case Event.Host   => route(SceneRoute.HostWorld)
        case Event.GoBack => route(SceneRoute.Main)
      }

      scene

    case SceneRoute.JoinWorld =>
      import Menus.JoinWorldChooserMenu.Event

      val (tx, rx) = Channel[Event]()
      val scene = Menus.JoinWorldChooserMenu(tx)

      rx.onEvent {
        case Event.Join(address, port) =>
          println(s"Will connect to: $address at port $port")
          route(SceneRoute.Game(null, WorldSettings.none, isHosting = false, isOnline = true, (address, port)))
        case Event.GoBack => route(SceneRoute.Multiplayer)
      }

      scene

    case SceneRoute.HostWorld =>
      import Menus.HostWorldChooserMenu.Event

      val (tx, rx) = Channel[Event]()
      val scene = Menus.HostWorldChooserMenu(saveFolder, fs)(tx)

      rx.onEvent {
        case Event.Host(f) =>
          println(s"Hosting world from ${f.saveFile.getName}")
          route(SceneRoute.Game(f.saveFile, WorldSettings.none, isHosting = true, isOnline = true, null))
        case Event.GoBack => route(SceneRoute.Multiplayer)
      }

      scene

    case SceneRoute.Settings =>
      new Menus.SettingsMenu(() => route(SceneRoute.Main))

    case SceneRoute.Game(saveDir, settings, isHosting, isOnline, serverLocation) =>
      val client = if serverLocation != null then GameClient(serverLocation._1, serverLocation._2) else null

      val worldProvider =
        if isHosting then {
          WorldProviderFromFile(saveDir, settings, fs)
        } else {
          RemoteWorldProvider(client)
        }

      val networkHandler = NetworkHandler(isHosting, isOnline, worldProvider, client)
      val (tx, rx) = Channel[GameScene.Event]()
      val scene = GameScene.create(networkHandler, kb, BlockTextureLoader.instance, window.windowSize, audioSystem)(tx)

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
  }
}
