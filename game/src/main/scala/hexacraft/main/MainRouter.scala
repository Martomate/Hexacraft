package hexacraft.main

import hexacraft.game.{GameClient, GameKeyboard, GameScene, NetworkHandler, RemoteWorldProvider, WorldProviderFromFile}
import hexacraft.gui.Scene
import hexacraft.infra.fs.{BlockTextureLoader, FileSystem}
import hexacraft.infra.window.CursorMode
import hexacraft.menu.*
import hexacraft.util.Tracker
import hexacraft.world.settings.WorldSettings

import java.io.File

object MainRouter {
  enum Event:
    case SceneChanged(newScene: Scene)
    case QuitRequested
}

class MainRouter(
    saveFolder: File,
    multiplayerEnabled: Boolean,
    fs: FileSystem,
    window: GameWindow,
    kb: GameKeyboard
)(eventListener: Tracker[MainRouter.Event]) {

  def route(sceneRoute: SceneRoute): Unit = eventListener.notify(MainRouter.Event.SceneChanged(createScene(sceneRoute)))

  private def createScene(sceneRoute: SceneRoute): Scene = sceneRoute match
    case SceneRoute.Main =>
      import MainMenu.Event.*

      MainMenu(multiplayerEnabled):
        case Play        => route(SceneRoute.WorldChooser)
        case Multiplayer => route(SceneRoute.Multiplayer)
        case Settings    => route(SceneRoute.Settings)
        case Quit        => eventListener.notify(MainRouter.Event.QuitRequested)

    case SceneRoute.WorldChooser =>
      import WorldChooserMenu.Event.*

      WorldChooserMenu(saveFolder, fs):
        case StartGame(saveDir, settings) =>
          route(SceneRoute.Game(saveDir, settings, isHosting = true, isOnline = false, null))
        case CreateNewWorld => route(SceneRoute.NewWorld)
        case GoBack         => route(SceneRoute.Main)

    case SceneRoute.NewWorld =>
      import NewWorldMenu.Event.*

      NewWorldMenu(saveFolder):
        case StartGame(saveDir, settings) =>
          route(SceneRoute.Game(saveDir, settings, isHosting = true, isOnline = false, null))
        case GoBack => route(SceneRoute.WorldChooser)

    case SceneRoute.Multiplayer =>
      import MultiplayerMenu.Event.*

      MultiplayerMenu:
        case Join   => route(SceneRoute.JoinWorld)
        case Host   => route(SceneRoute.HostWorld)
        case GoBack => route(SceneRoute.Main)

    case SceneRoute.JoinWorld =>
      import JoinWorldChooserMenu.Event.*

      JoinWorldChooserMenu:
        case Join(address, port) =>
          println(s"Will connect to: $address at port $port")
          route(SceneRoute.Game(null, WorldSettings.none, isHosting = false, isOnline = true, (address, port)))
        case GoBack => route(SceneRoute.Multiplayer)

    case SceneRoute.HostWorld =>
      import HostWorldChooserMenu.Event.*

      HostWorldChooserMenu(saveFolder, fs):
        case Host(f) =>
          println(s"Hosting world from ${f.saveFile.getName}")
          route(SceneRoute.Game(f.saveFile, WorldSettings.none, isHosting = true, isOnline = true, null))
        case GoBack => route(SceneRoute.Multiplayer)

    case SceneRoute.Settings =>
      new SettingsMenu(() => route(SceneRoute.Main))

    case SceneRoute.Game(saveDir, settings, isHosting, isOnline, serverLocation) =>
      val worldProvider =
        if isHosting
        then WorldProviderFromFile(saveDir, settings, fs)
        else RemoteWorldProvider(GameClient(serverLocation._1, serverLocation._2))

      GameScene(NetworkHandler(isHosting, isOnline, worldProvider), kb, BlockTextureLoader.instance, window.windowSize):
        case GameScene.Event.GameQuit =>
          route(SceneRoute.Main)
          System.gc()
        case GameScene.Event.CursorCaptured =>
          window.setCursorMode(CursorMode.Disabled)
        case GameScene.Event.CursorReleased =>
          window.setCursorMode(CursorMode.Normal)
}
