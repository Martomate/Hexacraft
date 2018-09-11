package com.martomate.hexacraft.gui.menu.main

import java.io.File

import com.martomate.hexacraft.gui.comp.{Button, Label, ScrollPane}
import com.martomate.hexacraft.gui.location.LocationInfo16x9
import com.martomate.hexacraft.gui.menu.MenuScene
import com.martomate.hexacraft.scene.GameScene
import com.martomate.hexacraft.world.settings.WorldSettings
import com.martomate.hexacraft.{GameWindow, Main}

class WorldChooserMenu(implicit window: GameWindow) extends MenuScene {
  addComponent(new Label("Choose world", LocationInfo16x9(0, 0.85f, 1, 0.15f), 6).withColor(1, 1, 1))
  private val scrollPane = new ScrollPane(window, LocationInfo16x9(0.285f, 0.225f, 0.43f, 0.635f))
  getWorlds.zipWithIndex.map {
    case (f, i) =>
      new Button(f.name, LocationInfo16x9(0.3f, 0.75f - 0.1f * i, 0.4f, 0.075f))({
        window.scenes.popScenesUntil(MenuScene.isMainMenu)
        window.scenes.pushScene(new GameScene(f.saveFile, WorldSettings.none))
      })
  }.foreach(b => scrollPane.addComponent(b))
  addComponent(scrollPane)

  addComponent(new Button("Back to menu", LocationInfo16x9(0.3f, 0.1f, 0.19f, 0.1f))({ window.scenes.popScene() }))
  addComponent(new Button("New world", LocationInfo16x9(0.51f, 0.1f, 0.19f, 0.1f))({ window.scenes.pushScene(new NewWorldMenu) }))

  private def getWorlds: Seq[WorldInfo] = {
    val baseFolder = new File(Main.saveFolder, "saves")
    if (baseFolder.exists()) baseFolder.listFiles().filter(f => new File(f, "world.dat").exists()).map(saveFile => WorldInfo(saveFile))
    else Seq.empty[WorldInfo]
  }

}
