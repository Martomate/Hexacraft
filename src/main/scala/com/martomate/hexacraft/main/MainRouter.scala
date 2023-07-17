package com.martomate.hexacraft.main

import com.martomate.hexacraft.{GameKeyboard, GameMouse, GameWindow}
import com.martomate.hexacraft.game.{GameScene, WorldProviderFromFile}
import com.martomate.hexacraft.gui.{Scene, WindowExtras}
import com.martomate.hexacraft.menu.{
  HostWorldChooserMenu,
  JoinWorldChooserMenu,
  MainMenu,
  MultiplayerMenu,
  NewWorldMenu,
  SettingsMenu,
  WorldChooserMenu
}
import com.martomate.hexacraft.util.Tracker
import com.martomate.hexacraft.world.block.{BlockLoader, Blocks}
import com.martomate.hexacraft.world.settings.WorldSettings

import java.io.File

object MainRouter {
  enum Event:
    case SceneChanged(newScene: Scene)
    case QuitRequested
}

class MainRouter(saveFolder: File, multiplayerEnabled: Boolean)(eventListener: Tracker[MainRouter.Event])(using
    GameWindow,
    GameMouse,
    GameKeyboard,
    WindowExtras
) {
  def route(sceneRoute: SceneRoute): Unit = eventListener.notify(MainRouter.Event.SceneChanged(createScene(sceneRoute)))

  private def createScene(sceneRoute: SceneRoute)(using GameWindow, GameMouse): Scene = sceneRoute match
    case SceneRoute.Main =>
      import MainMenu.Event.*

      MainMenu(multiplayerEnabled):
        case Play        => route(SceneRoute.WorldChooser)
        case Multiplayer => route(SceneRoute.Multiplayer)
        case Settings    => route(SceneRoute.Settings)
        case Quit        => eventListener.notify(MainRouter.Event.QuitRequested)
    case SceneRoute.WorldChooser =>
      import WorldChooserMenu.Event.*

      WorldChooserMenu(saveFolder):
        case StartGame(saveFile, settings) => route(SceneRoute.Game(saveFile, settings))
        case CreateNewWorld                => route(SceneRoute.NewWorld)
        case GoBack                        => route(SceneRoute.Main)
    case SceneRoute.NewWorld =>
      import NewWorldMenu.Event.*

      NewWorldMenu(saveFolder):
        case StartGame(saveFile, settings) => route(SceneRoute.Game(saveFile, settings))
        case GoBack                        => route(SceneRoute.WorldChooser)
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
        case GoBack => route(SceneRoute.Multiplayer)
    case SceneRoute.HostWorld =>
      import HostWorldChooserMenu.Event.*

      HostWorldChooserMenu(saveFolder):
        case Host(f) =>
          println(s"Hosting world from ${f.saveFile.getName}")
          route(SceneRoute.Game(f.saveFile, WorldSettings.none))
        case GoBack => route(SceneRoute.Multiplayer)
    case SceneRoute.Settings =>
      new SettingsMenu(() => route(SceneRoute.Main))
    case SceneRoute.Game(saveFile, settings) =>
      given BlockLoader = BlockLoader.instance // this loads it to memory
      given Blocks = new Blocks

      GameScene(new WorldProviderFromFile(saveFile, settings)):
        case GameScene.Event.QuitGame =>
          route(SceneRoute.Main)
          System.gc()
}
