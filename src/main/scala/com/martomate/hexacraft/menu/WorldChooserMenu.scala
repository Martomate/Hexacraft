package com.martomate.hexacraft.menu

import com.martomate.hexacraft.game.{GameScene, WorldProviderFromFile}
import com.martomate.hexacraft.gui.{GameWindowExtended, LocationInfo, MenuScene}
import com.martomate.hexacraft.gui.comp.{Button, Label, ScrollPane}
import com.martomate.hexacraft.world.block.Blocks
import com.martomate.hexacraft.world.settings.WorldSettings

import java.io.File

class WorldChooserMenu(saveFolder: File)(using window: GameWindowExtended, Blocks: Blocks) extends MenuScene {
  addComponent(
    new Label("Choose world", LocationInfo.from16x9(0, 0.85f, 1, 0.15f), 6).withColor(1, 1, 1)
  )

  addComponent(makeScrollPane)

  addComponent(Button("Back to menu", LocationInfo.from16x9(0.3f, 0.05f, 0.19f, 0.1f)) {
    window.scenes.popScene()
  })
  addComponent(Button("New world", LocationInfo.from16x9(0.51f, 0.05f, 0.19f, 0.1f)) {
    window.scenes.pushScene(new NewWorldMenu(saveFolder))
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
      window.scenes.popScenesUntil(MenuScene.isMainMenu)

      val worldProvider = new WorldProviderFromFile(world.saveFile, WorldSettings.none)
      window.scenes.pushScene(new GameScene(worldProvider))
    }
  }

  private def getWorlds: Seq[WorldInfo] = {
    val baseFolder = new File(saveFolder, "saves")
    if (baseFolder.exists()) {
      for (saveFile <- saveFoldersSortedBy(baseFolder, -_.lastModified))
        yield WorldInfo.fromFile(saveFile)
    } else {
      Seq.empty[WorldInfo]
    }
  }

  private def saveFoldersSortedBy[S](baseFolder: File, sortFunc: File => S)(using
      Ordering[S]
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
