package com.martomate.hexacraft.gui.comp

import com.martomate.hexacraft.gui.{CharEvent, KeyEvent, MouseClickEvent, MouseMoveEvent, ScrollEvent}
import com.martomate.hexacraft.gui.location.LocationInfo
import org.joml.Vector4f
import org.lwjgl.opengl.GL11

import scala.collection.mutable.ArrayBuffer

class ScrollPane(_location: LocationInfo) extends Component(_location) {
  private var xOffset: Float = 0
  private var yOffset: Float = 0

  private val components: ArrayBuffer[Component] = ArrayBuffer.empty[Component]

  def addComponent(comp: Component): Unit = components.append(comp)

  override def render(transformation: GUITransformation): Unit = {
    Component.drawRect(location, transformation.x, transformation.y, new Vector4f(0, 0, 0, 0.4f))

    val contentTransformation = transformation.offset(this.xOffset, this.yOffset)
    val loc = location.inScaledScreenCoordinates
    GL11.glScissor(loc._1, loc._2, loc._3, loc._4)
    GL11.glEnable(GL11.GL_SCISSOR_TEST)
    components.foreach(_.render(contentTransformation))
    GL11.glDisable(GL11.GL_SCISSOR_TEST)
    super.render(contentTransformation)
  }

  override def onScrollEvent(event: ScrollEvent): Boolean = {
    if (containsMouse) {
      this.xOffset += event.xoffset * 0.05f
      this.yOffset -= event.yoffset * 0.05f
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
      components.exists(_.onMouseClickEvent(event.withMouseTranslation(-xOffset, -yOffset)))
    } else false
  }

  override def onMouseMoveEvent(event: MouseMoveEvent): Boolean = {
    if (containsMouse) {
      components.exists(_.onMouseMoveEvent(event))
    } else false
  }

  def containsMouse: Boolean = location.containsMouse(0, 0)

  override def unload(): Unit = {
    components.foreach(_.unload())
    super.unload()
  }

}
