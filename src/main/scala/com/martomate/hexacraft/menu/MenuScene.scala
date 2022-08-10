package com.martomate.hexacraft.menu

import com.martomate.hexacraft.GameWindow
import com.martomate.hexacraft.gui.comp._
import com.martomate.hexacraft.gui.location.{LocationInfo, LocationInfoIdentity}
import com.martomate.hexacraft.resource.TextureSingle
import com.martomate.hexacraft.scene.Scene

abstract class MenuScene(implicit window: GameWindow) extends Scene with SubComponents {
  protected var hasDefaultBackground: Boolean = true
  def isMainMenu: Boolean = false

  override def render(transformation: GUITransformation): Unit = {
    if (hasDefaultBackground)
      Component.drawImage(
        MenuScene.entireBackground,
        transformation.x,
        transformation.y,
        TextureSingle.getTexture("textures/gui/menu/background")
      )
    super.render(transformation)
  }
}

object MenuScene {
  def entireBackground(implicit window: GameWindow): LocationInfo =
    new LocationInfoIdentity(-window.aspectRatio, -1, window.aspectRatio * 2, 2)

  val isMainMenu: Scene => Boolean = {
    case m: MenuScene => m.isMainMenu
    case _            => false
  }
}
