package hexagon.gui.comp

import fontMeshCreator.GUIText
import hexagon.event.MouseClickEvent
import org.joml.Vector4f
import org.lwjgl.glfw.GLFW

class Button(text: String, _location: LocationInfo)(clickAction: =>Unit) extends Component(_location) {
  {
    var fontSize = 2.0f
    var guiText: GUIText = Component.makeText(text, location, fontSize)
    addText(guiText)
    while (guiText.getNumberOfLines > 1) {
      removeText(guiText)
      fontSize *= 0.8f
      guiText = Component.makeText(text, location, fontSize)
      addText(guiText)
    }
  }

  override def render(transformation: GUITransformation): Unit = {
    if (location.containsMouse(transformation.x, transformation.y)) {
      Component.drawRect(location, transformation.x, transformation.y, new Vector4f(0.7f, 0.7f, 0.7f, 0.75f))
    } else {
      Component.drawRect(location, transformation.x, transformation.y, new Vector4f(0.6f, 0.6f, 0.6f, 0.75f))
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
