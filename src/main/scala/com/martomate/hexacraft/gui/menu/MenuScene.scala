package com.martomate.hexacraft.gui.menu

import com.martomate.hexacraft.Main
import com.martomate.hexacraft.gui.comp._
import com.martomate.hexacraft.gui.location.{LocationInfo, LocationInfoIdentity}
import com.martomate.hexacraft.resource.TextureSingle
import com.martomate.hexacraft.scene.Scene

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