package com.martomate.hexacraft.gui.comp

import com.martomate.hexacraft.gui.MouseClickEvent
import com.martomate.hexacraft.gui.location.LocationInfo
import org.joml.Vector4f
import org.lwjgl.glfw.GLFW

object Button {
  def apply(text: String, location: LocationInfo)(clickAction: => Unit): Button =
    new Button(text, location, clickAction)
}

class Button(text: String, location: LocationInfo, clickAction: => Unit) extends Component {
  addText(Component.makeText(text, location, 4.0f).setTextAndFitSize(text, 4.0f))

  override def render(transformation: GUITransformation): Unit = {
    if (location.containsMouse(transformation.x, transformation.y)) {
      Component.drawRect(
        location,
        transformation.x,
        transformation.y,
        new Vector4f(0.7f, 0.7f, 0.7f, 0.75f)
      )
    } else {
      Component.drawRect(
        location,
        transformation.x,
        transformation.y,
        new Vector4f(0.6f, 0.6f, 0.6f, 0.75f)
      )
    }
    super.render(transformation)
  }

  override def onMouseClickEvent(event: MouseClickEvent): Boolean = {
    if (event.action == GLFW.GLFW_RELEASE && location.containsPoint(event.mousePos)) {
      clickAction
      true
    } else false
  }
}
