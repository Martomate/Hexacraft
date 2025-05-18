package hexacraft.gui.comp

import hexacraft.gui.{Event, LocationInfo, RenderContext}
import hexacraft.infra.window.MouseAction

import org.joml.Vector4f

object Button {
  def apply(text: String, bounds: LocationInfo, disabled: Boolean = false)(clickAction: => Unit): Button = {
    new Button(text, bounds, clickAction, disabled)
  }
}

class Button(text: String, val bounds: LocationInfo, clickAction: => Unit, disabled: Boolean)
    extends Component
    with Boundable {
  addText(Component.makeText(text, bounds, 4.0f, shadow = true).setTextAndFitSize(text, 4.0f))

  override def render(context: RenderContext): Unit = {
    val mousePos = context.heightNormalizedMousePos
    val containsMouse = bounds.containsPoint(mousePos.x - context.offset.x, mousePos.y - context.offset.y)

    val color =
      if disabled then {
        new Vector4f(0.3f, 0.3f, 0.3f, 0.8f)
      } else if containsMouse then {
        new Vector4f(0.5f, 0.5f, 0.5f, 0.8f)
      } else {
        new Vector4f(0.4f, 0.4f, 0.4f, 0.8f)
      }

    if disabled then {
      Component.drawRect(bounds, context.offset.x, context.offset.y, color, context.windowAspectRatio)
    } else {
      Component.drawFancyRect(bounds, context.offset.x, context.offset.y, color, context.windowAspectRatio)
    }
    super.render(context)
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
