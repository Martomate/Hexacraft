package com.martomate.hexacraft.menu

import com.martomate.hexacraft.{GameKeyboard, GameMouse, GameWindow}
import com.martomate.hexacraft.game.{GameScene, WorldProviderFromFile}
import com.martomate.hexacraft.gui.{LocationInfo, MenuScene, WindowExtras, WindowScenes}
import com.martomate.hexacraft.gui.comp.{Button, Label}
import com.martomate.hexacraft.world.WorldProvider
import com.martomate.hexacraft.world.block.Blocks
import com.martomate.hexacraft.world.settings.WorldSettings

import java.io.File

class MultiplayerMenu(saveFolder: File)(using
    mouse: GameMouse,
    keyboard: GameKeyboard,
    window: GameWindow,
    Blocks: Blocks,
    windowExtras: WindowExtras,
    scenes: WindowScenes
) extends MenuScene {
  addComponent(new Label("Multiplayer", LocationInfo.from16x9(0, 0.8f, 1, 0.2f), 10).withColor(1, 1, 1))
  addComponent(Button("Join", LocationInfo.from16x9(0.4f, 0.55f, 0.2f, 0.1f)) {
    scenes.pushScene(new JoinWorldChooserMenu((address, port) => {
      println(s"Will connect to: $address at port $port")
      scenes.popScenesUntil(MenuScene.isMainMenu)
    }))
  })
  addComponent(Button("Host", LocationInfo.from16x9(0.4f, 0.4f, 0.2f, 0.1f)) {
    scenes.pushScene(
      new HostWorldChooserMenu(
        saveFolder,
        f => {
          println(s"Hosting world from ${f.saveFile.getName}")
          scenes.popScenesUntil(MenuScene.isMainMenu)
          scenes.pushScene(new GameScene(new WorldProviderFromFile(f.saveFile, WorldSettings.none)))
        }
      )
    )
  })
  addComponent(Button("Back", LocationInfo.from16x9(0.4f, 0.05f, 0.2f, 0.1f))(scenes.popScene()))
}
