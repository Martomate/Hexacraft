package hexagon.gui.comp

import hexagon.Main
import org.joml.Vector4f
import org.lwjgl.glfw.GLFW

class Button(text: String, _location: LocationInfo)(clickAction: =>Unit) extends Component(_location) {
  private val guiText = Component.makeText(text, location, 2)
  addText(guiText)

  override def render(): Unit = {
    if (location.containsMouse) {
      Component.drawRect(location, new Vector4f(0.7f, 0.7f, 0.7f, 0.75f))
    } else {
      Component.drawRect(location, new Vector4f(0.6f, 0.6f, 0.6f, 0.75f))
    }
    super.render()
  }

  override def onMouseClickEvent(button: Int, action: Int, mods: Int): Unit = {
    if (action == GLFW.GLFW_RELEASE && location.containsMouse) clickAction
  }
}
