package com.martomate.hexacraft.menu.main

import com.martomate.hexacraft.game.GameScene
import com.martomate.hexacraft.gui.comp.{Button, Label, ScrollPane}
import com.martomate.hexacraft.gui.location.LocationInfo16x9
import com.martomate.hexacraft.menu.MenuScene
import com.martomate.hexacraft.scene.GameWindowExtended
import com.martomate.hexacraft.world.settings.{WorldProviderFromFile, WorldSettings}

import java.io.File

class WorldChooserMenu(implicit window: GameWindowExtended) extends MenuScene {
  addComponent(
    new Label("Choose world", LocationInfo16x9(0, 0.85f, 1, 0.15f), 6).withColor(1, 1, 1)
  )

  addComponent(makeScrollPane)

  addComponent(Button("Back to menu", LocationInfo16x9(0.3f, 0.05f, 0.19f, 0.1f)) {
    window.scenes.popScene()
  })
  addComponent(Button("New world", LocationInfo16x9(0.51f, 0.05f, 0.19f, 0.1f)) {
    window.scenes.pushScene(new NewWorldMenu)
  })

  private def makeScrollPane: ScrollPane = {
    val scrollPane = new ScrollPane(LocationInfo16x9(0.285f, 0.225f, 0.43f, 0.635f))

    val buttons = for ((f, i) <- getWorlds.zipWithIndex) yield makeWorldButton(f, i)

    for (b <- buttons) scrollPane.addComponent(b)

    scrollPane
  }

  private def makeWorldButton(world: WorldInfo, listIndex: Int): Button = {
    Button(world.name, LocationInfo16x9(0.3f, 0.75f - 0.1f * listIndex, 0.4f, 0.075f)) {
      window.scenes.popScenesUntil(MenuScene.isMainMenu)
      window.scenes.pushScene(
        new GameScene(new WorldProviderFromFile(world.saveFile, WorldSettings.none))
      )
    }
  }

  private def getWorlds: Seq[WorldInfo] = {
    val baseFolder = new File(window.saveFolder, "saves")
    if (baseFolder.exists()) {
      for (saveFile <- saveFoldersSortedBy(baseFolder, -_.lastModified)) yield WorldInfo(saveFile)
    } else {
      Seq.empty[WorldInfo]
    }
  }

  private def saveFoldersSortedBy[S](baseFolder: File, sortFunc: File => S)(implicit
      ord: Ordering[S]
  ): Seq[File] = {
    baseFolder
      .listFiles()
      .map(worldFolder => (worldFolder, new File(worldFolder, "world.dat")))
      .filter(t => t._2.exists())
      .sortBy(t => sortFunc(t._2))
      .map(_._1)
      .toSeq
  }
}
