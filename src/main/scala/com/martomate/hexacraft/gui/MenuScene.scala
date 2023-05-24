package com.martomate.hexacraft.gui

import com.martomate.hexacraft.GameWindow
import com.martomate.hexacraft.gui.{LocationInfo, Scene}
import com.martomate.hexacraft.gui.comp.*
import com.martomate.hexacraft.renderer.TextureSingle

abstract class MenuScene(using window: GameWindow) extends Scene with SubComponents {
  protected var hasDefaultBackground: Boolean = true
  def isMainMenu: Boolean = false

  override def render(transformation: GUITransformation): Unit = {
    if (hasDefaultBackground)
      Component.drawImage(
        LocationInfo(-window.aspectRatio, -1, window.aspectRatio * 2, 2),
        transformation.x,
        transformation.y,
        TextureSingle.getTexture("textures/gui/menu/background")
      )
    super.render(transformation)
  }
}

object MenuScene {
  val isMainMenu: Scene => Boolean = {
    case m: MenuScene => m.isMainMenu
    case _            => false
  }
}
