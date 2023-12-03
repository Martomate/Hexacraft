package hexacraft.gui

import hexacraft.gui.comp.*
import hexacraft.renderer.TextureSingle

abstract class MenuScene extends Scene with SubComponents {
  protected var hasDefaultBackground: Boolean = true
  def isMainMenu: Boolean = false

  override def render(transformation: GUITransformation)(using context: RenderContext): Unit = {
    if (hasDefaultBackground)
      Component.drawImage(
        LocationInfo(-context.windowAspectRatio, -1, context.windowAspectRatio * 2, 2),
        transformation.x,
        transformation.y,
        TextureSingle.getTexture("textures/gui/menu/background"),
        context.windowAspectRatio
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
