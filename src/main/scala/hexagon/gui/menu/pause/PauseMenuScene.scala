package hexagon.gui.menu.pause

import hexagon.Main
import hexagon.gui.comp.{Button, LocationInfo}
import hexagon.gui.menu.MenuScene


class PauseMenuScene extends MenuScene {
  addComponent(new Button("Back to game", LocationInfo(0.4f, 0.25f, 0.2f, 0.1f))({
    Main.popScene()
  }))
  addComponent(new Button("Back to menu", LocationInfo(0.4f, 0.55f, 0.2f, 0.1f))({
    Main.popScene()
    Main.popScene()
  }))

  override def render(): Unit = {
    super.render()
  }

  override def onReloadedResources(): Unit = ()
}
