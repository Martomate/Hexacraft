package com.martomate.hexacraft.gui.comp

import com.martomate.hexacraft.GameWindow
import com.martomate.hexacraft.gui.{LocationInfo, MouseClickEvent}

import org.joml.Vector4f
import org.lwjgl.glfw.GLFW

object Button:
  def apply(text: String, location: LocationInfo)(clickAction: => Unit)(using GameWindow): Button =
    new Button(text, location, clickAction)

class Button(text: String, location: LocationInfo, clickAction: => Unit)(using GameWindow)
    extends Component
    with Boundable:
  addText(Component.makeText(text, location, 4.0f).setTextAndFitSize(text, 4.0f))

  override def bounds: LocationInfo = location

  override def render(transformation: GUITransformation): Unit =
    val containsMouse = location.containsMouse(transformation.x, transformation.y)

    val color =
      if containsMouse
      then new Vector4f(0.7f, 0.7f, 0.7f, 0.75f)
      else new Vector4f(0.6f, 0.6f, 0.6f, 0.75f)

    Component.drawRect(location, transformation.x, transformation.y, color)
    super.render(transformation)

  override def onMouseClickEvent(event: MouseClickEvent): Boolean =
    val mouseReleased = event.action == GLFW.GLFW_RELEASE
    val containsMouse = location.containsPoint(event.mousePos)

    if mouseReleased && containsMouse
    then
      clickAction
      true
    else false
