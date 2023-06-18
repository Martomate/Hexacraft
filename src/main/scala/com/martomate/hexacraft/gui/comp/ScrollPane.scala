package com.martomate.hexacraft.gui.comp

import com.martomate.hexacraft.{GameMouse, GameWindow}
import com.martomate.hexacraft.gui.{Event, LocationInfo}
import com.martomate.hexacraft.util.{MathUtils, OpenGL}

import org.joml.Vector4f
import scala.collection.mutable.ArrayBuffer

class ScrollPane(
    location: LocationInfo,
    padding: Float = 0,
    enableHorizontalScroll: Boolean = false
)(using mouse: GameMouse, window: GameWindow)
    extends Component:
  private var xOffset: Float = 0
  private var yOffset: Float = 0

  private val components: ArrayBuffer[Component with Boundable] = ArrayBuffer.empty

  def addComponent(comp: Component with Boundable): Unit = components.append(comp)

  override def render(transformation: GUITransformation)(using GameWindow): Unit =
    Component.drawRect(location, transformation.x, transformation.y, new Vector4f(0, 0, 0, 0.4f))

    val contentTransformation = transformation.offset(this.xOffset, this.yOffset)
    val loc = location.inScaledScreenCoordinates(window.framebufferSize)
    OpenGL.glScissor(loc.x, loc.y, loc.w, loc.h)
    OpenGL.glEnable(OpenGL.State.ScissorTest)
    components.foreach(_.render(contentTransformation))
    OpenGL.glDisable(OpenGL.State.ScissorTest)
    super.render(contentTransformation)

  override def handleEvent(event: Event): Boolean = event match
    case event: Event.CharEvent => components.exists(_.handleEvent(event))
    case event: Event.KeyEvent  => components.exists(_.handleEvent(event))
    case event: Event.MouseClickEvent =>
      if containsMouse
      then components.exists(_.handleEvent(event.withMouseTranslation(-xOffset, -yOffset)))
      else false
    case Event.ScrollEvent(xOffset, yOffset) =>
      if containsMouse then
        val boxBounds = location
        val contentBounds = calcContentBounds()

        if enableHorizontalScroll then this.xOffset += xOffset * 0.05f
        this.yOffset -= yOffset * 0.05f

        if boxBounds.h - contentBounds.h < 2 * padding then
          val limitTop = boxBounds.y + boxBounds.h - padding
          val limitBottom = boxBounds.y + padding
          val contentTop = contentBounds.y + contentBounds.h
          val contentBottom = contentBounds.y

          this.yOffset = MathUtils.clamp(this.yOffset, limitTop - contentTop, limitBottom - contentBottom)
        true
      else false

  private def containsMouse: Boolean =
    val mousePos = mouse.heightNormalizedPos(window.windowSize)
    location.containsPoint(mousePos.x, mousePos.y)

  override def unload(): Unit =
    components.foreach(_.unload())
    super.unload()

  private def calcContentBounds(): LocationInfo =
    components
      .map(_.bounds)
      .reduceOption(LocationInfo.hull)
      .getOrElse(LocationInfo(0, 0, 0, 0))
