package hexagon.gui.menu.main

import java.io.File

import hexagon.Main
import hexagon.gui.comp.{Button, LocationInfo, TextField}
import hexagon.gui.menu.MenuScene
import hexagon.scene.GameScene

class NewWorldMenu extends MenuScene{
  // TODO: add textfields and other settings
  private val nameTF = new TextField(LocationInfo(0.3f, 0.55f, 0.4f, 0.1f))
  addComponent(nameTF)

  addComponent(new Button("Cancel", LocationInfo(0.3f, 0.1f, 0.19f, 0.1f))({ Main.popScene() }))
  addComponent(new Button("Create world", LocationInfo(0.51f, 0.1f, 0.19f, 0.1f))({
    Main.popScenesUntilMainMenu()
    Main.pushScene(new GameScene(new File(Main.saveFolder, "saves/" + nameTF.text)))
  }))


  override def onReloadedResources(): Unit = ()
}
