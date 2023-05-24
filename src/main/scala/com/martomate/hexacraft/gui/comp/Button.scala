package com.martomate.hexacraft.gui.comp

import com.martomate.hexacraft.{GameMouse, GameWindow}
import com.martomate.hexacraft.gui.{LocationInfo, Event}

import org.joml.Vector4f
import org.lwjgl.glfw.GLFW

object Button:
  def apply(text: String, bounds: LocationInfo)(clickAction: => Unit)(using GameMouse, GameWindow): Button =
    new Button(text, bounds, clickAction)

class Button(text: String, val bounds: LocationInfo, clickAction: => Unit)(using mouse: GameMouse, window: GameWindow)
    extends Component
    with Boundable:
  addText(Component.makeText(text, bounds, 4.0f).setTextAndFitSize(text, 4.0f))

  override def render(transformation: GUITransformation): Unit =
    val mousePos = mouse.heightNormalizedPos(window.windowSize)
    val containsMouse = bounds.containsPoint(mousePos.x - transformation.x, mousePos.y - transformation.y)

    val color =
      if containsMouse
      then new Vector4f(0.7f, 0.7f, 0.7f, 0.75f)
      else new Vector4f(0.6f, 0.6f, 0.6f, 0.75f)

    Component.drawRect(bounds, transformation.x, transformation.y, color)
    super.render(transformation)

  override def onMouseClickEvent(event: Event.MouseClickEvent): Boolean =
    val mouseReleased = event.action == Event.MouseAction.Release
    val containsMouse = bounds.containsPoint(event.mousePos)

    if mouseReleased && containsMouse
    then
      clickAction
      true
    else false
