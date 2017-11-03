package hexagon.gui.menu.main

import java.io.File

import hexagon.Main
import hexagon.gui.comp.{Button, Label, LocationInfo}
import hexagon.gui.menu.MenuScene
import hexagon.scene.GameScene
import hexagon.world.WorldSettings

class WorldChooserMenu extends MenuScene {
  addComponent(new Label("Choose world", LocationInfo(0, 0.85f, 1, 0.15f), 3).withColor(1, 1, 1))
  getWorlds.zipWithIndex.map {
    case (f, i) =>
      new Button(f.getName, LocationInfo(0.3f, 0.75f - 0.1f * i, 0.4f, 0.075f))({
        Main.popScenesUntilMainMenu()
        Main.pushScene(new GameScene(f, WorldSettings.none))
      })
  }.foreach(addComponent)

  addComponent(new Button("Back to menu", LocationInfo(0.3f, 0.1f, 0.19f, 0.1f))({ Main.popScene() }))
  addComponent(new Button("New world", LocationInfo(0.51f, 0.1f, 0.19f, 0.1f))({ Main.pushScene(new NewWorldMenu) }))

  private def getWorlds: Seq[File] = {
    val baseFolder = new File(Main.saveFolder, "saves")
    if (baseFolder.exists()) baseFolder.listFiles().filter(f => new File(f, "world.dat").exists())
    else Seq.empty[File]
  }

  override def onReloadedResources(): Unit = ()
}
