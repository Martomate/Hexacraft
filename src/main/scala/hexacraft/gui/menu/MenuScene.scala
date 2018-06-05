package hexacraft.gui.menu

import hexacraft.Main
import hexacraft.gui.comp._
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
  def entireBackground: LocationInfo = new LocationInfoIdentity(-Main.aspectRatio, -1, Main.aspectRatio * 2, 2)
}