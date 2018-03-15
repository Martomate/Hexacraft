package hexacraft.gui.menu

import hexacraft.gui.comp.{Component, GUITransformation, LocationInfo, SubComponents}
import hexacraft.resource.TextureSingle
import hexacraft.scene.Scene

abstract class MenuScene extends Scene with SubComponents {
  protected var hasDefaultBackground: Boolean = true

  override def render(transformation: GUITransformation): Unit = {
    if (hasDefaultBackground) Component.drawImage(MenuScene.entireBackground, transformation.x, transformation.y, TextureSingle.getTexture("textures/gui/menu/background"))
    super.render(transformation)
  }

}

object MenuScene {
  val entireBackground: LocationInfo = LocationInfo(0, 0, 1, 1)
}