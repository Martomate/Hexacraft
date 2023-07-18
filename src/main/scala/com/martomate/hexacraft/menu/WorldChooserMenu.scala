package com.martomate.hexacraft.menu

import com.martomate.hexacraft.{GameMouse, GameWindow}
import com.martomate.hexacraft.gui.{LocationInfo, MenuScene}
import com.martomate.hexacraft.gui.comp.{Button, Label, ScrollPane}
import com.martomate.hexacraft.infra.fs.FileSystem
import com.martomate.hexacraft.menu.WorldChooserMenu.Event
import com.martomate.hexacraft.world.settings.WorldSettings

import java.io.File
import java.nio.file.Path

object WorldChooserMenu {
  enum Event:
    case StartGame(saveDir: File, settings: WorldSettings)
    case CreateNewWorld
    case GoBack
}

class WorldChooserMenu(saveFolder: File, fs: FileSystem)(onEvent: WorldChooserMenu.Event => Unit)(using
    GameMouse,
    GameWindow
) extends MenuScene {

  addComponent(
    new Label("Choose world", LocationInfo.from16x9(0, 0.85f, 1, 0.15f), 6).withColor(1, 1, 1)
  )

  addComponent(makeScrollPane)

  addComponent(Button("Back to menu", LocationInfo.from16x9(0.3f, 0.05f, 0.19f, 0.1f)) {
    onEvent(Event.GoBack)
  })
  addComponent(Button("New world", LocationInfo.from16x9(0.51f, 0.05f, 0.19f, 0.1f)) {
    onEvent(Event.CreateNewWorld)
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
      onEvent(Event.StartGame(world.saveFile, WorldSettings.none))
    }
  }

  private def getWorlds: Seq[WorldInfo] = {
    val baseFolder = new File(saveFolder, "saves")
    if (fs.exists(baseFolder.toPath)) {
      for (saveFile <- saveFoldersSortedBy(baseFolder, p => -fs.lastModified(p).toEpochMilli))
        yield WorldInfo.fromFile(saveFile.toFile, fs)
    } else {
      Seq.empty[WorldInfo]
    }
  }

  private def saveFoldersSortedBy[S](baseFolder: File, sortFunc: Path => S)(using
      Ordering[S]
  ): Seq[Path] = {
    fs.listFiles(baseFolder.toPath)
      .map(worldFolder => (worldFolder, worldFolder.resolve("world.dat")))
      .filter(t => fs.exists(t._2))
      .sortBy(t => sortFunc(t._2))
      .map(_._1)
  }
}
