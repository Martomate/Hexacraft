package hexacraft.gui.comp

import hexacraft.event.{CharEvent, KeyEvent, MouseClickEvent, ScrollEvent}
import org.joml.Vector4f
import org.lwjgl.opengl.GL11

import scala.collection.mutable.ArrayBuffer

class ScrollPane(_location: LocationInfo) extends Component(_location) {
  private var (xoffset, yoffset): (Float, Float) = (0, 0)

  private val components: ArrayBuffer[Component] = ArrayBuffer.empty[Component]

  def addComponent(comp: Component): Unit = components.append(comp)

  override def render(transformation: GUITransformation): Unit = {
    val xoffset = transformation.x
    val yoffset = transformation.y
    val newTr = GUITransformation(xoffset + this.xoffset, yoffset + this.yoffset)
    Component.drawRect(location, xoffset, yoffset, new Vector4f(0, 0, 0, 0.4f))
    val loc = location.inScreenCoordinates
    GL11.glScissor(loc._1, loc._2, loc._3, loc._4)
    GL11.glEnable(GL11.GL_SCISSOR_TEST)
    components.foreach(_.render(newTr))
    GL11.glDisable(GL11.GL_SCISSOR_TEST)
    super.render(newTr)
  }

  override def onScrollEvent(event: ScrollEvent): Boolean = {
    if (containsMouse) {
      this.xoffset += event.xoffset * 0.05f
      this.yoffset -= event.yoffset * 0.05f
      true
    } else components.exists(_.onScrollEvent(event))
  }

  override def onKeyEvent(event: KeyEvent): Boolean = {
    components.exists(_.onKeyEvent(event))
  }

  override def onCharEvent(event: CharEvent): Boolean = {
    components.exists(_.onCharEvent(event))
  }

  override def onMouseClickEvent(event: MouseClickEvent): Boolean = {
    if (containsMouse) {
      components.exists(_.onMouseClickEvent(event.withMouseTranslation(-xoffset, -yoffset)))
    } else false
  }

  def containsMouse: Boolean = location.containsMouse(0, 0)

}
