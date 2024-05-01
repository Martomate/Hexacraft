package hexacraft.main

import hexacraft.game.*
import hexacraft.game.Menus.JoinWorldChooserMenu
import hexacraft.gui.Scene
import hexacraft.infra.audio.AudioSystem
import hexacraft.infra.fs.FileSystem
import hexacraft.infra.window.CursorMode
import hexacraft.util.Channel
import hexacraft.world.WorldSettings

import java.io.File

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
        case Event.StartGame(saveDir, settings) =>
          route(SceneRoute.Game(saveDir, settings, isHosting = true, isOnline = false, null))
        case Event.CreateNewWorld => route(SceneRoute.NewWorld)
        case Event.GoBack         => route(SceneRoute.Main)
      }

      scene

    case SceneRoute.NewWorld =>
      import Menus.NewWorldMenu.Event

      val (scene, rx) = Menus.NewWorldMenu.create(saveFolder)

      rx.onEvent {
        case Event.StartGame(saveDir, settings) =>
          route(SceneRoute.Game(saveDir, settings, isHosting = true, isOnline = false, null))
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

      val (scene, rx) = Menus.JoinWorldChooserMenu.create()

      rx.onEvent {
        case Event.Join(address, port) =>
          println(s"Will connect to: $address at port $port")
          route(SceneRoute.Game(null, WorldSettings.none, isHosting = false, isOnline = true, (address, port)))
        case Event.GoBack => route(SceneRoute.Multiplayer)
      }

      scene

    case SceneRoute.HostWorld =>
      import Menus.HostWorldChooserMenu.Event

      val (scene, rx) = Menus.HostWorldChooserMenu.create(saveFolder, fs)

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
      val (serverIp, serverPort) = if serverLocation != null then serverLocation else ("localhost", 1234)

      val (scene, rx) =
        if isHosting then {
          GameScene.createHostedGame(
            serverIp,
            serverPort,
            isOnline,
            null,
            kb,
            BlockTextureLoader.instance,
            window.windowSize,
            audioSystem,
            WorldProviderFromFile(saveDir, settings, fs)
          )
        } else {
          GameScene.createRemoteGame(
            serverIp,
            serverPort,
            isOnline,
            null,
            kb,
            BlockTextureLoader.instance,
            window.windowSize,
            audioSystem
          )
        }

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
