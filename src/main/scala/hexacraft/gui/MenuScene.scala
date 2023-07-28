package hexacraft.gui

import hexacraft.GameWindow
import hexacraft.gui.{LocationInfo, Scene}
import hexacraft.gui.comp.*
import hexacraft.renderer.TextureSingle

abstract class MenuScene extends Scene with SubComponents {
  protected var hasDefaultBackground: Boolean = true
  def isMainMenu: Boolean = false

  override def render(transformation: GUITransformation)(using window: GameWindow): Unit = {
    if (hasDefaultBackground)
      Component.drawImage(
        LocationInfo(-window.aspectRatio, -1, window.aspectRatio * 2, 2),
        transformation.x,
        transformation.y,
        TextureSingle.getTexture("textures/gui/menu/background"),
        window.aspectRatio
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
