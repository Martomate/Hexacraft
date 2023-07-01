package com.martomate.hexacraft.gui.comp

import com.martomate.hexacraft.{GameMouse, GameWindow}
import com.martomate.hexacraft.gui.{Event, LocationInfo}

import org.joml.Vector4f

object Button:
  def apply(text: String, bounds: LocationInfo)(clickAction: => Unit)(using GameMouse): Button =
    new Button(text, bounds, clickAction)

class Button(text: String, val bounds: LocationInfo, clickAction: => Unit)(using mouse: GameMouse)
    extends Component
    with Boundable:
  addText(Component.makeText(text, bounds, 4.0f).setTextAndFitSize(text, 4.0f))

  override def render(transformation: GUITransformation)(using window: GameWindow): Unit =
    val mousePos = mouse.heightNormalizedPos(window.windowSize)
    val containsMouse = bounds.containsPoint(mousePos.x - transformation.x, mousePos.y - transformation.y)

    val color =
      if containsMouse
      then new Vector4f(0.7f, 0.7f, 0.7f, 0.75f)
      else new Vector4f(0.6f, 0.6f, 0.6f, 0.75f)

    Component.drawRect(bounds, transformation.x, transformation.y, color)
    super.render(transformation)

  override def handleEvent(event: Event): Boolean = event match
    case Event.MouseClickEvent(_, action, _, mousePos) =>
      val mouseReleased = action == Event.MouseAction.Release
      val containsMouse = bounds.containsPoint(mousePos)

      if mouseReleased && containsMouse
      then
        clickAction
        true
      else false
    case _ => super.handleEvent(event)
