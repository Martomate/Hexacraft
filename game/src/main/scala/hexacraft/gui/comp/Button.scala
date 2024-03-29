package hexacraft.gui.comp

import hexacraft.gui.{Event, LocationInfo, RenderContext}
import hexacraft.infra.window.MouseAction

import org.joml.Vector4f

object Button {
  def apply(text: String, bounds: LocationInfo)(clickAction: => Unit): Button = {
    new Button(text, bounds, clickAction)
  }
}

class Button(text: String, val bounds: LocationInfo, clickAction: => Unit) extends Component with Boundable {
  addText(Component.makeText(text, bounds, 4.0f, shadow = true).setTextAndFitSize(text, 4.0f))

  override def render(transformation: GUITransformation)(using context: RenderContext): Unit = {
    val mousePos = context.heightNormalizedMousePos
    val containsMouse = bounds.containsPoint(mousePos.x - transformation.x, mousePos.y - transformation.y)

    val color =
      if containsMouse then {
        new Vector4f(0.5f, 0.5f, 0.5f, 0.8f)
      } else {
        new Vector4f(0.4f, 0.4f, 0.4f, 0.8f)
      }

    Component.drawFancyRect(bounds, transformation.x, transformation.y, color, context.windowAspectRatio)
    super.render(transformation)
  }

  override def handleEvent(event: Event): Boolean = event match {
    case Event.MouseClickEvent(_, action, _, mousePos) =>
      val mouseReleased = action == MouseAction.Release
      val containsMouse = bounds.containsPoint(mousePos)

      if mouseReleased && containsMouse then {
        clickAction
        true
      } else {
        false
      }
    case _ =>
      super.handleEvent(event)
  }
}
