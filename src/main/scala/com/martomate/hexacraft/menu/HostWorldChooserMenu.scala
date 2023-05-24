package com.martomate.hexacraft.menu

import com.martomate.hexacraft.{GameMouse, GameWindow}
import com.martomate.hexacraft.gui.{LocationInfo, MenuScene, WindowScenes}
import com.martomate.hexacraft.gui.comp.{Button, Label, ScrollPane}
import com.martomate.hexacraft.menu.WorldInfo

import java.io.File
import scala.util.Random

class HostWorldChooserMenu(saveFolder: File)(using mouse: GameMouse, window: GameWindow, scenes: WindowScenes)
    extends MenuScene {
  addComponent(
    new Label("Choose world", LocationInfo.from16x9(0, 0.85f, 1, 0.15f), 6).withColor(1, 1, 1)
  )
  private val scrollPane = new ScrollPane(LocationInfo.from16x9(0.285f, 0.225f, 0.43f, 0.635f))

  getWorlds.zipWithIndex
    .map { case (f, i) =>
      Button(f.name, LocationInfo.from16x9(0.3f, 0.75f - 0.1f * i, 0.4f, 0.075f)) {
        val id = registerWorld(f.name)
        println("The server got id: " + id)
//        window.scenes.popScenesUntil(MenuScene.isMainMenu)
//        window.scenes.pushScene(new GameScene(f.saveFile, WorldSettings.none))
        // TODO: the network manager should repeatedly connect to the server registry.
        //  This will be blocking until a client wants to connect or after a timeout
        //  If this is not done in a certain time period the server will be deregistered from the server registry
      }
    }
    .foreach(b => scrollPane.addComponent(b))
  addComponent(scrollPane)

  addComponent(Button("Back to menu", LocationInfo.from16x9(0.3f, 0.05f, 0.4f, 0.1f)) {
    scenes.popScene()
  })

  private def getWorlds: Seq[WorldInfo] = {
    val baseFolder = new File(saveFolder, "saves")
    if (baseFolder.exists()) {
      baseFolder
        .listFiles()
        .filter(f => new File(f, "world.dat").exists())
        .map(saveFile => WorldInfo.fromFile(saveFile))
        .toSeq
    } else {
      Seq.empty[WorldInfo]
    }
  }

  def registerWorld(name: String): Long = {
    // TODO: connect to server registry to register the server
    new Random().nextLong()
  }

}
